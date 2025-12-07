package com.example.backend.controller;

import com.example.backend.model.dto.*;
import com.example.backend.service.DownloadQueueService;
import com.example.backend.service.extractor.VideoExtractor;
import com.example.backend.service.extractor.VideoExtractorFactory;
import com.example.backend.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Slf4j
@CrossOrigin(origins = "*")
public class DownloadController {
    
    @Autowired
    private VideoExtractorFactory extractorFactory;
    
    @Autowired
    private DownloadQueueService queueService;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @PostMapping("/info")
    public ResponseEntity<ApiResponseDto<VideoInfoDto>> getVideoInfo(
            @Valid @RequestBody DownloadRequestDto request,
            HttpServletRequest httpRequest) {
        
        try {
            log.info("Video info request from IP: {} for URL: {}", 
                    httpRequest.getRemoteAddr(), request.getUrl());
            
            // Get appropriate extractor
            VideoExtractor extractor = extractorFactory.getExtractorForUrl(request.getUrl());
            
            // Extract video info
            VideoInfoDto videoInfo = extractor.extractInfo(request.getUrl());
            
            return ResponseEntity.ok(ApiResponseDto.success(videoInfo));
            
        } catch (Exception e) {
            log.error("Error getting video info: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error(
                        "Failed to get video info: " + e.getMessage(),
                        "INFO_EXTRACTION_ERROR",
                        400
                    ));
        }
    }
    
    @PostMapping("/download")
    public ResponseEntity<ApiResponseDto<TaskStatusDto>> startDownload(
            @Valid @RequestBody DownloadRequestDto request,
            HttpServletRequest httpRequest) {
        
        try {
            log.info("Download request from IP: {} for URL: {}", 
                    httpRequest.getRemoteAddr(), request.getUrl());
            
            // Get appropriate extractor
            VideoExtractor extractor = extractorFactory.getExtractorForUrl(request.getUrl());
            String serviceName = extractor.getServiceName();
            
            // Extract video info first
            VideoInfoDto videoInfo = extractor.extractInfo(request.getUrl());
            
            // Create task status DTO
            TaskStatusDto taskStatus = TaskStatusDto.builder()
                .taskId(java.util.UUID.randomUUID().toString())
                .url(request.getUrl())
                .title(videoInfo.getTitle())
                .status("PENDING")
                .progress(0)
                .serviceName(serviceName)
                .formatId(request.getFormatId())
                .quality(request.getQuality())
                .createdAt(java.time.LocalDateTime.now())
                .build();
            
            // Add to queue
            queueService.addToQueue(taskStatus.getTaskId());
            
            return ResponseEntity.ok(ApiResponseDto.success("Download task created", taskStatus));
            
        } catch (Exception e) {
            log.error("Error starting download: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error(
                        "Failed to start download: " + e.getMessage(),
                        "DOWNLOAD_START_ERROR",
                        400
                    ));
        }
    }
    
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponseDto<TaskStatusDto>> getTaskStatus(@PathVariable String taskId) {
        // In a real implementation, this would fetch from database
        // For now, return a mock response
        TaskStatusDto taskStatus = TaskStatusDto.builder()
            .taskId(taskId)
            .status("PROCESSING")
            .progress(50)
            .build();
        
        return ResponseEntity.ok(ApiResponseDto.success(taskStatus));
    }
    
    @GetMapping("/downloads/{filename:.+}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String filename,
            HttpServletRequest request) {
        
        try {
            Path filePath = fileStorageService.getFilePath(filename);
            
            if (!fileStorageService.fileExists(filename)) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new UrlResource(filePath.toUri());
            
            // Determine content type
            String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
                    
        } catch (MalformedURLException e) {
            log.error("Malformed URL for file: {}", filename, e);
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.error("Error accessing file: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/queue/status")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> getQueueStatus() {
        Map<String, Object> status = queueService.getQueueStatus();
        return ResponseEntity.ok(ApiResponseDto.success(status));
    }
    
    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponseDto<Void>> cancelDownload(@PathVariable String taskId) {
        try {
            queueService.cancelTask(taskId);
            return ResponseEntity.ok(ApiResponseDto.success("Download cancelled", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error(
                        "Failed to cancel download: " + e.getMessage(),
                        "CANCEL_ERROR",
                        400
                    ));
        }
    }
}