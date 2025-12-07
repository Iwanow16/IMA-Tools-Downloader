package com.example.backend.service;

import com.example.backend.model.dto.TaskStatusDto;
import com.example.backend.model.entity.DownloadTaskEntity;
import com.example.backend.repository.DownloadTaskRepository;
import com.example.backend.service.processor.VideoProcessor;
import com.example.backend.service.processor.VideoProcessorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
@Slf4j
public class DownloadQueueService {
    
    @Autowired
    private DownloadTaskRepository taskRepository;
    
    @Autowired
    private VideoProcessorFactory processorFactory;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    private final ExecutorService downloadExecutor;
    private final Map<String, Future<?>> activeTasks;
    private final int MAX_CONCURRENT_DOWNLOADS = 5;
    
    public DownloadQueueService() {
        this.downloadExecutor = Executors.newFixedThreadPool(MAX_CONCURRENT_DOWNLOADS);
        this.activeTasks = new ConcurrentHashMap<>();
    }
    
    @Async
    public void addToQueue(String taskId) {
        DownloadTaskEntity task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            log.error("Task not found: {}", taskId);
            return;
        }
        
        // Update task status
        task.setStatus("QUEUED");
        taskRepository.save(task);
        
        // Submit to executor
        Future<?> future = downloadExecutor.submit(() -> processTask(task));
        activeTasks.put(taskId, future);
        
        log.info("Task {} added to queue", taskId);
    }
    
    private void processTask(DownloadTaskEntity task) {
        try {
            // Update status to processing
            task.setStatus("PROCESSING");
            taskRepository.save(task);
            
            // Get processor for the service
            VideoProcessor processor = processorFactory.getProcessor(task.getServiceName());
            
            // Generate output path
            String outputPath = fileStorageService.generateOutputPath(
                task.getServiceName(),
                task.getFilename(),
                task.getFormatId()
            );
            
            // Process download
            TaskStatusDto result = processor.processDownload(
                task.getUrl(),
                task.getFormatId(),
                task.getQuality(),
                outputPath,
                task.getTaskId()
            );
            
            // Update task with result
            task.setStatus(result.getStatus());
            task.setProgress(result.getProgress());
            task.setFilePath(outputPath);
            task.setDownloadUrl("/api/downloads/" + Paths.get(outputPath).getFileName().toString());
            
            if (result.getFileSize() != null) {
                task.setFileSize(result.getFileSize());
            }
            
            taskRepository.save(task);
            log.info("Task {} processed successfully", task.getTaskId());
            
        } catch (Exception e) {
            log.error("Error processing task {}: {}", task.getTaskId(), e.getMessage(), e);
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            taskRepository.save(task);
        } finally {
            // Remove from active tasks
            activeTasks.remove(task.getTaskId());
        }
    }
    
    public void cancelTask(String taskId) {
        Future<?> future = activeTasks.get(taskId);
        if (future != null) {
            future.cancel(true);
            activeTasks.remove(taskId);
            
            DownloadTaskEntity task = taskRepository.findById(taskId).orElse(null);
            if (task != null) {
                task.setStatus("CANCELLED");
                taskRepository.save(task);
            }
        }
    }
    
    public int getQueueSize() {
        return activeTasks.size();
    }
    
    public int getActiveDownloadsCount() {
        return activeTasks.size();
    }
    
    public Map<String, Object> getQueueStatus() {
        Map<String, Object> status = new ConcurrentHashMap<>();
        status.put("activeDownloads", getActiveDownloadsCount());
        status.put("maxConcurrentDownloads", MAX_CONCURRENT_DOWNLOADS);
        status.put("queueSize", taskRepository.countByStatus("QUEUED"));
        status.put("pendingSize", taskRepository.countByStatus("PENDING"));
        return status;
    }
}