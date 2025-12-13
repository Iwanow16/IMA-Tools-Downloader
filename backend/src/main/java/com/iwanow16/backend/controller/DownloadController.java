package com.iwanow16.backend.controller;

import com.iwanow16.backend.extractor.VideoExtractorService;
import com.iwanow16.backend.model.dto.*;
import com.iwanow16.backend.service.DownloadQueueService;
import com.iwanow16.backend.service.FileStorageService;
import com.iwanow16.backend.service.strategy.DownloadStrategyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;

@RestController
@RequestMapping("/api")
public class DownloadController {

    @Autowired
    private VideoExtractorService extractorService;

    @Autowired
    private DownloadQueueService queueService;

    @Autowired
    private FileStorageService storage;

    @Autowired
    private DownloadStrategyFactory strategyFactory;

    private String getClientIp(HttpServletRequest request) {
        // Check X-Forwarded-For header (for proxied requests)
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        // Fallback to remote address
        return request.getRemoteAddr();
    }

    @GetMapping("/info")
    public ResponseEntity<ApiResponseDto<VideoInfoDto>> info(@RequestParam String url) {
        try {
            // Получить подходящий экстрактор
            VideoInfoDto info = extractorService.extractInfo(url);
            return ResponseEntity.ok(ApiResponseDto.success(info));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponseDto.error("Unsupported URL: " + e.getMessage(), 400));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseDto.error("Failed to extract info: " + e.getMessage(), 400));
        }
    }

    @PostMapping("/download")
    public ResponseEntity<ApiResponseDto<TaskStatusDto>> download(@RequestBody DownloadRequestDto req, HttpServletRequest request) {
        String ip = getClientIp(request);
        
        // Валидация URL
        if (req.getUrl() == null || req.getUrl().length() > 1000) {
            return ResponseEntity.badRequest().body(ApiResponseDto.error("Invalid URL", 400));
        }

        // Проверить, поддерживается ли URL
        if (!strategyFactory.isSupported(req.getUrl())) {
            return ResponseEntity.badRequest().body(ApiResponseDto.error(
                    "URL not supported. Supported services: " + strategyFactory.getSupportedServices(), 400));
        }

        TaskStatusDto t = queueService.submitDownload(req.getUrl(), ip, req.getFormatId(), req.getQuality());
        return ResponseEntity.ok(ApiResponseDto.success("Task created", t));
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponseDto<TaskStatusDto>> getTask(@PathVariable String taskId, HttpServletRequest request) {
        String ip = getClientIp(request);
        TaskStatusDto t = queueService.getTask(taskId, ip);
        if (t == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ApiResponseDto.success(t));
    }

    @GetMapping("/tasks")
    public ResponseEntity<ApiResponseDto<Object>> listTasks(HttpServletRequest request) {
        String ip = getClientIp(request);
        return ResponseEntity.ok(ApiResponseDto.success(queueService.getQueueStatus(ip)));
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponseDto<Void>> cancel(@PathVariable String taskId, HttpServletRequest request) {
        String ip = getClientIp(request);
        queueService.cancelTask(taskId, ip);
        return ResponseEntity.ok(ApiResponseDto.success(null));
    }

    @GetMapping("/downloads/{filename}")
    public ResponseEntity<FileSystemResource> downloadFile(@PathVariable String filename, HttpServletRequest request) {
        if (!storage.fileExists(filename)) {
            return ResponseEntity.notFound().build();
        }
        
        String clientIp = getClientIp(request);
        
        // Check if file belongs to a task completed by current user
        java.util.List<TaskStatusDto> userTasks = queueService.getQueueStatus(clientIp);
        boolean hasAccess = userTasks.stream()
                .anyMatch(task -> task.getFilename() != null && 
                                 task.getFilename().equals(filename) && 
                                 "completed".equals(task.getStatus()));
        
        if (!hasAccess) {
            return ResponseEntity.status(403).build(); // Forbidden: file doesn't belong to user or not completed
        }
        
        File file = storage.getFile(filename);
        FileSystemResource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName())
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    private String extractTaskIdFromFilename(String filename) {
        // filename format: "dl-<taskId>.ext"
        if (filename.startsWith("dl-") && filename.contains(".")) {
            return filename.substring(3, filename.lastIndexOf("."));
        }
        return filename;
    }
}