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
        String id = UUID.randomUUID().toString();
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

        executor.submit(() -> runDownloadTask(id, url, clientIp, formatId, quality));
        return t;
    }

    private void runDownloadTask(String taskId, String url, String clientIp, String formatId, String quality) {
        Semaphore ipSem = ipSemaphores.computeIfAbsent(clientIp, k -> new Semaphore(props.getMaxConcurrentPerIp()));
        TaskStatusDto t = tasks.get(taskId);
        try {
            globalSemaphore.acquire();
            ipSem.acquire();
            t.setStatus("downloading");

            // Получить подходящую стратегию для URL
            DownloadStrategy strategy = strategyFactory.getStrategy(url);
            log.info("[{}] Using strategy: {}", taskId, strategy.getServiceName());

            // Скачать файл
            Path downloadDir = storage.getStorageDir();
            Path downloadedFile = strategy.download(url, downloadDir, formatId, taskId);

            // Сохранить информацию о файле
            String filename = downloadedFile.getFileName().toString();
            t.setFilename(filename);
            t.setStatus("completed");
            t.setProgress(100);
            t.setCompletedAt(OffsetDateTime.now());
            log.info("[{}] Download completed: {}", taskId, filename);

        } catch (Exception e) {
            log.error("[{}] Error in download task", taskId, e);
            t.setStatus("failed");
            t.setProgress(0);
            t.setFailedAt(OffsetDateTime.now());
            t.setError(e.getMessage());
        } finally {
            ipSemaphores.computeIfPresent(clientIp, (k, sem) -> { sem.release(); return sem; });
            globalSemaphore.release();
        }
    }

    public TaskStatusDto getTask(String id, String clientIp) {
        TaskStatusDto t = tasks.get(id);
        if (t != null && !t.getClientIp().equals(clientIp)) {
            return null; // Access denied: task belongs to different client
        }
        return t;
    }

    public List<TaskStatusDto> getQueueStatus(String clientIp) {
        return tasks.values().stream()
                .filter(task -> task.getClientIp().equals(clientIp))
                .toList();
    }

    public void cancelTask(String taskId, String clientIp) {
        TaskStatusDto t = tasks.get(taskId);
        if (t != null && t.getClientIp().equals(clientIp)) {
            t.setStatus("cancelled");
        }
    }
}
