package com.iwanow16.backend.service;

import com.iwanow16.backend.config.DownloaderProperties;
import com.iwanow16.backend.model.dto.TaskStatusDto;
import com.iwanow16.backend.service.strategy.DownloadStrategyFactory;
import com.iwanow16.backend.service.strategy.DownloadStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@Service
public class DownloadQueueService {
    private static final Logger log = LoggerFactory.getLogger(DownloadQueueService.class);

    @Autowired
    private DownloaderProperties props;

    @Autowired
    private FileStorageService storage;

    @Autowired
    private DownloadStrategyFactory strategyFactory;

    private ExecutorService executor;
    private Semaphore globalSemaphore;
    private final ConcurrentMap<String, Semaphore> ipSemaphores = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TaskStatusDto> tasks = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() throws Exception {
        executor = Executors.newFixedThreadPool(Math.max(2, props.getMaxConcurrentDownloads()));
        globalSemaphore = new Semaphore(props.getMaxConcurrentDownloads());
        storage.ensureDirectories();
    }

    public TaskStatusDto submitDownload(String url, String clientIp, String formatId, String quality) {
        return submitDownloadWithOptions(url, clientIp, formatId, quality, false, null, null, false, null);
    }

    public TaskStatusDto submitDownloadWithOptions(String url, String clientIp, String formatId, String quality,
                                                   boolean timeRangeEnabled, String startTime, String endTime,
                                                   boolean frameExtractionEnabled, String frameTime) {
        String id = UUID.randomUUID().toString();
        log.info("📥 New download submitted | TaskID: {} | Format: {} | Quality: {} | IP: {} | TimeRange: {} | Frame: {}", 
                id, formatId, quality, clientIp, timeRangeEnabled, frameExtractionEnabled);
        
        TaskStatusDto t = new TaskStatusDto();
        t.setTaskId(id);
        t.setUrl(url);
        t.setStatus("pending");
        t.setProgress(0);
        t.setFormatId(formatId);
        t.setQuality(quality);
        t.setClientIp(clientIp);
        t.setCreatedAt(OffsetDateTime.now());
        tasks.put(id, t);

        executor.submit(() -> runDownloadTask(id, url, clientIp, formatId, quality, 
                timeRangeEnabled, startTime, endTime, frameExtractionEnabled, frameTime));
        log.debug("⏳ Task queued for processing | TaskID: {}", id);
        return t;
    }

    private void runDownloadTask(String taskId, String url, String clientIp, String formatId, String quality) {
        runDownloadTask(taskId, url, clientIp, formatId, quality, false, null, null, false, null);
    }

    private void runDownloadTask(String taskId, String url, String clientIp, String formatId, String quality,
                                boolean timeRangeEnabled, String startTime, String endTime,
                                boolean frameExtractionEnabled, String frameTime) {
        Semaphore ipSem = ipSemaphores.computeIfAbsent(clientIp, k -> new Semaphore(props.getMaxConcurrentPerIp()));
        TaskStatusDto t = tasks.get(taskId);
        long taskStartTime = System.currentTimeMillis();
        
        try {
            log.debug("⏳ Acquiring semaphores for TaskID: {} | IP: {}", taskId, clientIp);
            globalSemaphore.acquire();
            ipSem.acquire();
            t.setStatus("downloading");
            log.info("⬇️ Starting download | TaskID: {} | URL: {} | Format: {}", taskId, url, formatId);

            // Получить подходящую стратегию для URL
            DownloadStrategy strategy = strategyFactory.getStrategy(url);
            log.debug("🎬 Using strategy: {} | TaskID: {}", strategy.getServiceName(), taskId);

            // Скачать файл в зависимости от опций
            Path downloadDir = storage.getStorageDir();
            long downloadStartTime = System.currentTimeMillis();
            Path downloadedFile;

            if (frameExtractionEnabled && frameTime != null) {
                // Извлечение кадра
                log.info("📷 Extracting frame | TaskID: {} | Time: {}s", taskId, frameTime);
                downloadedFile = strategy.extractFrame(url, downloadDir, taskId, frameTime);
            } else if (timeRangeEnabled && startTime != null && endTime != null) {
                // Загрузка временного диапазона
                log.info("⏱️  Downloading time range | TaskID: {} | From: {}s | To: {}s", 
                        taskId, startTime, endTime);
                downloadedFile = strategy.downloadTimeRange(url, downloadDir, formatId, taskId, startTime, endTime);
            } else {
                // Обычная загрузка полного видео
                downloadedFile = strategy.download(url, downloadDir, formatId, taskId);
            }

            long downloadDuration = System.currentTimeMillis() - downloadStartTime;

            // Сохранить информацию о файле
            String filename = downloadedFile.getFileName().toString();
            t.setFilename(filename);
            t.setStatus("completed");
            t.setProgress(100);
            t.setCompletedAt(OffsetDateTime.now());
            long totalDuration = System.currentTimeMillis() - taskStartTime;
            log.info("✅ Download completed | TaskID: {} | Filename: {} | Download: {}ms | Total: {}ms", 
                    taskId, filename, downloadDuration, totalDuration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - taskStartTime;
            log.error("❌ Download failed | TaskID: {} | Duration: {}ms | Error: {}", taskId, duration, e.getMessage(), e);
            t.setStatus("failed");
            t.setProgress(0);
            t.setFailedAt(OffsetDateTime.now());
            t.setError(e.getMessage());
        } finally {
            log.debug("🔓 Releasing semaphores for TaskID: {} | IP: {}", taskId, clientIp);
            ipSemaphores.computeIfPresent(clientIp, (k, sem) -> { sem.release(); return sem; });
            globalSemaphore.release();
        }
    }

    public TaskStatusDto getTask(String id, String clientIp) {
        TaskStatusDto t = tasks.get(id);
        if (t != null && !t.getClientIp().equals(clientIp)) {
            log.warn("🚫 Access denied | TaskID: {} | RequestIP: {} | TaskIP: {}", id, clientIp, t.getClientIp());
            return null; // Access denied: task belongs to different client
        }
        if (t != null) {
            log.debug("📋 Task status queried | TaskID: {} | Status: {} | Progress: {}%", id, t.getStatus(), t.getProgress());
        } else {
            log.debug("❓ Task not found | TaskID: {} | IP: {}", id, clientIp);
        }
        return t;
    }

    public List<TaskStatusDto> getQueueStatus(String clientIp) {
        List<TaskStatusDto> clientTasks = tasks.values().stream()
                .filter(task -> task.getClientIp().equals(clientIp))
                .toList();
        int pending = (int) clientTasks.stream().filter(t -> "pending".equals(t.getStatus())).count();
        int downloading = (int) clientTasks.stream().filter(t -> "downloading".equals(t.getStatus())).count();
        int completed = (int) clientTasks.stream().filter(t -> "completed".equals(t.getStatus())).count();
        log.debug("📋 Queue status retrieved | IP: {} | Total: {} | Pending: {} | Downloading: {} | Completed: {}", 
                clientIp, clientTasks.size(), pending, downloading, completed);
        return clientTasks;
    }

    public void cancelTask(String taskId, String clientIp) {
        TaskStatusDto t = tasks.get(taskId);
        if (t != null && t.getClientIp().equals(clientIp)) {
            String previousStatus = t.getStatus();
            t.setStatus("cancelled");
            log.info("⛔ Task cancelled | TaskID: {} | PreviousStatus: {} | IP: {}", taskId, previousStatus, clientIp);
        } else if (t == null) {
            log.warn("⛔ Cancel failed - task not found | TaskID: {} | IP: {}", taskId, clientIp);
        } else {
            log.warn("🚫 Cancel denied - access denied | TaskID: {} | RequestIP: {} | TaskIP: {}", taskId, clientIp, t.getClientIp());
        }
    }
}
