package com.iwanow16.backend.controller;

import com.iwanow16.backend.extractor.YtDlpVideoExtractor;
import com.iwanow16.backend.model.dto.*;
import com.iwanow16.backend.service.DownloadQueueService;
import com.iwanow16.backend.service.FileStorageService;
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
    private YtDlpVideoExtractor extractor;

    @Autowired
    private DownloadQueueService queueService;

    @Autowired
    private FileStorageService storage;

    @GetMapping("/info")
    public ResponseEntity<ApiResponseDto<VideoInfoDto>> info(@RequestParam String url) {
        try {
            VideoInfoDto info = extractor.extractInfo(url);
            return ResponseEntity.ok(ApiResponseDto.success(info));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseDto.error(e.getMessage(), 400));
        }
    }

    @PostMapping("/download")
    public ResponseEntity<ApiResponseDto<TaskStatusDto>> download(@RequestBody DownloadRequestDto req, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        // basic validation
        if (req.getUrl() == null || req.getUrl().length() > 1000) {
            return ResponseEntity.badRequest().body(ApiResponseDto.error("Invalid URL", 400));
        }

        TaskStatusDto t = queueService.submitDownload(req.getUrl(), ip, req.getFormatId(), req.getQuality());
        return ResponseEntity.ok(ApiResponseDto.success("Task created", t));
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponseDto<TaskStatusDto>> getTask(@PathVariable String taskId) {
        TaskStatusDto t = queueService.getTask(taskId);
        if (t == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ApiResponseDto.success(t));
    }

    @GetMapping("/tasks")
    public ResponseEntity<ApiResponseDto<Object>> listTasks() {
        return ResponseEntity.ok(ApiResponseDto.success(queueService.getQueueStatus()));
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponseDto<Void>> cancel(@PathVariable String taskId) {
        queueService.cancelTask(taskId);
        return ResponseEntity.ok(ApiResponseDto.success(null));
    }

    @GetMapping("/downloads/{filename}")
    public ResponseEntity<FileSystemResource> downloadFile(@PathVariable String filename) {
        if (!storage.fileExists(filename)) {
            return ResponseEntity.notFound().build();
        }
        File file = storage.getFile(filename);
        FileSystemResource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName())
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

}
