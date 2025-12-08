package com.iwanow16.backend.service;

import com.iwanow16.backend.config.DownloaderProperties;
import com.iwanow16.backend.model.dto.TaskStatusDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Service
public class DownloadQueueService {
    private static final Logger log = LoggerFactory.getLogger(DownloadQueueService.class);

    @Autowired
    private DownloaderProperties props;

    @Autowired
    private FileStorageService storage;

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
        t.setStatus("PENDING");
        t.setProgress(0);
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
            t.setStatus("PROCESSING");
            // Run download via script
            String safeName = "dl-" + taskId + ".%(ext)s";
            Path out = storage.getFilePath(safeName);
            t.setFilename(safeName);

            // Use system command yt-dlp via shell script
            ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c",
                    String.format("/app/scripts/yt-dlp-wrapper.sh '%s' '%s' '%s' '%s'", url, out.getParent().toString(), formatId == null ? "" : formatId, quality == null ? "" : quality));
            pb.redirectErrorStream(true);
            Process p = pb.start();
            // simple progress poll: just wait for completion
            int rc = p.waitFor();
            if (rc == 0) {
                t.setStatus("COMPLETED");
                t.setProgress(100);
            } else {
                t.setStatus("FAILED");
                t.setProgress(0);
            }
        } catch (Exception e) {
            log.error("Error in download task {}", taskId, e);
            t.setStatus("FAILED");
        } finally {
            ipSemaphores.computeIfPresent(clientIp, (k, sem) -> { sem.release(); return sem; });
            globalSemaphore.release();
        }
    }

    public TaskStatusDto getTask(String id) {
        return tasks.get(id);
    }

    public Map<String, Object> getQueueStatus() {
        return Map.of("queued", tasks.size());
    }

    public void cancelTask(String taskId) {
        // Not implemented: would track Process and kill it
        TaskStatusDto t = tasks.get(taskId);
        if (t != null) t.setStatus("CANCELLED");
    }
}
