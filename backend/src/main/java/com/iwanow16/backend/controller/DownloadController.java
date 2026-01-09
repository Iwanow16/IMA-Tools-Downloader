package com.iwanow16.backend.controller;

import com.iwanow16.backend.config.ServicesConfig;
import com.iwanow16.backend.extractor.VideoExtractorService;
import com.iwanow16.backend.model.dto.*;
import com.iwanow16.backend.service.DownloadQueueService;
import com.iwanow16.backend.service.FileStorageService;
import com.iwanow16.backend.service.strategy.DownloadStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(DownloadController.class);

    @Autowired
    private VideoExtractorService extractorService;

    @Autowired
    private DownloadQueueService queueService;

    @Autowired
    private FileStorageService storage;

    @Autowired
    private DownloadStrategyFactory strategyFactory;

    @Autowired
    private ServicesConfig servicesConfig;

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
            log.info("Fetching video info for URL: {}", url);
            
            // Determine which service is needed
            String serviceName = extractServiceName(url);
            
            // Check if service is enabled
            if (!servicesConfig.isServiceEnabled(serviceName)) {
                String message = String.format("Service '%s' is currently disabled", serviceName);
                log.warn(message);
                return ResponseEntity.status(503).body(ApiResponseDto.error(message + ". Contact administrator to enable.", 503));
            }
            
            VideoInfoDto info = extractorService.extractInfo(url);
            log.info("Successfully extracted info: title={}, formats={}", info.getTitle(), info.getFormats().size());
            ApiResponseDto<VideoInfoDto> response = ApiResponseDto.success(info);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Unsupported URL: {}", url, e);
            return ResponseEntity.badRequest().body(ApiResponseDto.error("Unsupported URL: " + e.getMessage(), 400));
        } catch (Exception e) {
            log.error("Failed to extract info for URL: {}", url, e);
            return ResponseEntity.badRequest().body(ApiResponseDto.error("Failed to extract info: " + e.getMessage(), 400));
        }
    }

    @GetMapping("/services")
    public ResponseEntity<ApiResponseDto<java.util.List<String>>> getSupportedServices() {
        log.info("Getting supported services list");
        java.util.List<String> allServices = strategyFactory.getSupportedServices();
        
        // Filter out disabled services
        java.util.List<String> services = allServices.stream()
                .filter(servicesConfig::isServiceEnabled)
                .toList();
        
        log.info("Available services: {} (all: {})", services, allServices);
        return ResponseEntity.ok(ApiResponseDto.success(services));
    }

    @PostMapping("/download")
    public ResponseEntity<ApiResponseDto<TaskStatusDto>> download(@RequestBody DownloadRequestDto req, HttpServletRequest request) {
        String ip = getClientIp(request);
        
        log.info("⬇️  Download request | IP: {} | URL: {} | Format: {} | Quality: {} | TimeRange: {} | Frame: {}", 
                ip, req.getUrl(), req.getFormatId(), req.getQuality(), 
                req.isTimeRangeEnabled(), req.isFrameExtractionEnabled());
        
        // Validate URL
        if (req.getUrl() == null || req.getUrl().length() > 1000) {
            log.warn("Invalid URL from {}", ip);
            return ResponseEntity.badRequest().body(ApiResponseDto.error("Invalid URL", 400));
        }

        // Check if URL is supported
        if (!strategyFactory.isSupported(req.getUrl())) {
            log.warn("Unsupported URL from {}: {}", ip, req.getUrl());
            return ResponseEntity.badRequest().body(ApiResponseDto.error(
                    "URL not supported. Please check if the URL is valid and belongs to a supported service.", 400));
        }

        // Check if service is enabled
        String serviceName = extractServiceName(req.getUrl());
        if (!servicesConfig.isServiceEnabled(serviceName)) {
            String message = String.format("Service '%s' is currently disabled", serviceName);
            log.warn(message);
            return ResponseEntity.status(503).body(ApiResponseDto.error(message + ". Contact administrator to enable.", 503));
        }

        TaskStatusDto t = queueService.submitDownloadWithOptions(
                req.getUrl(), ip, req.getFormatId(), req.getQuality(),
                req.isTimeRangeEnabled(), req.getStartTime(), req.getEndTime(),
                req.isFrameExtractionEnabled(), req.getFrameTime());
        
        log.info("Download task created | TaskID: {} | Status: {}", t.getTaskId(), t.getStatus());
        return ResponseEntity.ok(ApiResponseDto.success("Task created", t));
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponseDto<TaskStatusDto>> getTask(@PathVariable String taskId, HttpServletRequest request) {
        String ip = getClientIp(request);
        log.debug("Get task status | IP: {} | TaskID: {}", ip, taskId);
        
        TaskStatusDto t = queueService.getTask(taskId, ip);
        if (t == null) {
            log.warn("Task not found | TaskID: {}", taskId);
            return ResponseEntity.notFound().build();
        }
        
        log.debug("Task found | TaskID: {} | Status: {} | Progress: {}%", 
                taskId, t.getStatus(), t.getProgress());
        return ResponseEntity.ok(ApiResponseDto.success(t));
    }

    @GetMapping("/tasks")
    public ResponseEntity<ApiResponseDto<Object>> listTasks(HttpServletRequest request) {
        String ip = getClientIp(request);
        log.debug("List all tasks | IP: {}", ip);
        return ResponseEntity.ok(ApiResponseDto.success(queueService.getQueueStatus(ip)));
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponseDto<Void>> cancel(@PathVariable String taskId, HttpServletRequest request) {
        String ip = getClientIp(request);
        log.info("⛔ Cancel task | IP: {} | TaskID: {}", ip, taskId);
        
        try {
            queueService.cancelTask(taskId, ip);
            log.info("Task cancelled successfully | TaskID: {}", taskId);
            return ResponseEntity.ok(ApiResponseDto.success(null));
        } catch (Exception e) {
            log.error("Failed to cancel task | TaskID: {}", taskId, e);
            throw e;
        }
    }

    @GetMapping("/downloads/{filename}")
    public ResponseEntity<FileSystemResource> downloadFile(@PathVariable String filename, HttpServletRequest request) {
        log.debug("Download file request | Filename: {}", filename);
        
        if (!storage.fileExists(filename)) {
            log.warn("File not found | Filename: {} | IP: {}", filename, getClientIp(request));
            return ResponseEntity.notFound().build();
        }
        
        String clientIp = getClientIp(request);
        
        // Check if file belongs to a task completed by current user (optimized: check status first)
        java.util.List<TaskStatusDto> userTasks = queueService.getQueueStatus(clientIp);
        boolean hasAccess = userTasks.stream()
                .filter(task -> "completed".equals(task.getStatus()))
                .anyMatch(task -> task.getFilename() != null && task.getFilename().equals(filename));
        
        if (!hasAccess) {
            log.warn("Access denied | Filename: {} | IP: {}", filename, clientIp);
            return ResponseEntity.status(403).build(); // Forbidden: file doesn't belong to user or not completed
        }
        
        File file = storage.getFile(filename);
        long fileSize = file.length();
        log.info("Serving file | Filename: {} | Size: {} bytes | IP: {}", filename, fileSize, clientIp);
        
        FileSystemResource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName())
                .contentLength(fileSize)
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

    /**
     * Определить название сервиса по URL
     * @param url URL видео
     * @return название сервиса (youtube, vimeo, etc)
     */
    private String extractServiceName(String url) {
        if (url.contains("youtube.com") || url.contains("youtu.be")) {
            return "youtube";
        } else if (url.contains("bilibili.com") || url.contains("b23.tv")) {
            return "bilibili";
        } else if (url.contains("vimeo.com")) {
            return "vimeo";
        } else if (url.contains("twitch.tv")) {
            return "twitch";
        } else if (url.contains("instagram.com")) {
            return "instagram";
        } else if (url.contains("tiktok.com")) {
            return "tiktok";
        }
        return "unknown";
    }
}