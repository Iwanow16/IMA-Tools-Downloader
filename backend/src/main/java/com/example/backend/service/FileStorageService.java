package com.example.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {
    
    @Value("${downloader.output-dir:/app/downloads}")
    private String outputDir;
    
    @Value("${downloader.temp-dir:/app/temp}")
    private String tempDir;
    
    public String generateOutputPath(String serviceName, String filename, String formatId) {
        try {
            // Create service-specific directory
            Path serviceDir = Paths.get(outputDir, serviceName);
            if (!Files.exists(serviceDir)) {
                Files.createDirectories(serviceDir);
            }
            
            // Generate unique filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            
            String safeFilename;
            if (filename != null && !filename.trim().isEmpty()) {
                safeFilename = sanitizeFilename(filename);
            } else {
                safeFilename = "video_" + timestamp;
            }
            
            // Determine extension based on formatId or default
            String extension = getExtensionFromFormat(formatId);
            
            // Full path
            String fullFilename = String.format("%s_%s.%s", safeFilename, uniqueId, extension);
            return serviceDir.resolve(fullFilename).toAbsolutePath().toString();
            
        } catch (IOException e) {
            log.error("Error generating output path: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate output path", e);
        }
    }
    
    public String generateTempPath(String taskId, String extension) {
        try {
            Path tempDirPath = Paths.get(tempDir);
            if (!Files.exists(tempDirPath)) {
                Files.createDirectories(tempDirPath);
            }
            
            String filename = String.format("temp_%s.%s", taskId, extension);
            return tempDirPath.resolve(filename).toAbsolutePath().toString();
            
        } catch (IOException e) {
            log.error("Error generating temp path: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate temp path", e);
        }
    }
    
    public void cleanupTempFiles(int maxAgeHours) {
        try {
            Path tempDirPath = Paths.get(tempDir);
            if (!Files.exists(tempDirPath)) {
                return;
            }
            
            Files.list(tempDirPath)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    try {
                        long fileAgeHours = (System.currentTimeMillis() - 
                            Files.getLastModifiedTime(path).toMillis()) / (1000 * 60 * 60);
                        return fileAgeHours > maxAgeHours;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                        log.debug("Deleted temp file: {}", path);
                    } catch (IOException e) {
                        log.warn("Failed to delete temp file {}: {}", path, e.getMessage());
                    }
                });
            
        } catch (IOException e) {
            log.error("Error cleaning up temp files: {}", e.getMessage(), e);
        }
    }
    
    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9\\-_\\.\\s]", "")
                     .replaceAll("\\s+", "_")
                     .trim();
    }
    
    private String getExtensionFromFormat(String formatId) {
        if (formatId == null) {
            return "mp4";
        }
        
        // Map format IDs to extensions
        if (formatId.contains("mp4")) return "mp4";
        if (formatId.contains("webm")) return "webm";
        if (formatId.contains("mkv")) return "mkv";
        if (formatId.contains("mp3")) return "mp3";
        if (formatId.contains("m4a")) return "m4a";
        if (formatId.contains("wav")) return "wav";
        
        return "mp4"; // Default
    }
    
    public Path getFilePath(String filename) {
        return Paths.get(outputDir).resolve(filename);
    }
    
    public boolean fileExists(String filename) {
        Path filePath = getFilePath(filename);
        return Files.exists(filePath);
    }
    
    public long getFileSize(String filename) throws IOException {
        Path filePath = getFilePath(filename);
        return Files.size(filePath);
    }
}