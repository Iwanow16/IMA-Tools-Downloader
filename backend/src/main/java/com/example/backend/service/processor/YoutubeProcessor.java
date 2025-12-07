package com.example.backend.service.processor;

import com.example.backend.model.dto.TaskStatusDto;
import com.example.backend.exception.ServiceException;
import com.example.backend.util.ProcessExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class YoutubeProcessor implements VideoProcessor {
    
    @Value("${downloader.yt-dlp-path:/usr/local/bin/yt-dlp}")
    private String ytDlpPath;
    
    @Value("${downloader.output-dir:/app/downloads}")
    private String outputDir;
    
    @Override
    public TaskStatusDto processDownload(String url, String formatId, String quality,
                                        String outputPath, String taskId) throws ServiceException {
        log.info("Processing YouTube download for task: {}", taskId);
        
        try {
            Path outputDirPath = Paths.get(outputDir);
            if (!Files.exists(outputDirPath)) {
                Files.createDirectories(outputDirPath);
            }
            
            // Build command
            List<String> command = new ArrayList<>(Arrays.asList(
                ytDlpPath,
                "-f", formatId != null ? formatId : "best",
                "-o", outputPath,
                "--no-playlist",
                "--newline",
                "--progress"
            ));
            
            // Add quality options
            if (quality != null && quality.contains("audio")) {
                command.add("-x");
                command.add("--audio-format");
                command.add("mp3");
                command.add("--audio-quality");
                command.add("0");
            }
            
            command.add(url);
            
            // Execute download
            ProcessExecutor.ProcessResult result = ProcessExecutor.executeCommandWithProgress(
                command,
                progress -> {
                    // Progress callback can be used to update task status
                    log.debug("Download progress for task {}: {}%", taskId, progress);
                }
            );
            
            if (result.getExitCode() == 0) {
                log.info("YouTube download completed successfully for task: {}", taskId);
                return TaskStatusDto.builder()
                    .taskId(taskId)
                    .status("COMPLETED")
                    .progress(100)
                    .serviceName("youtube")
                    .build();
            } else {
                log.error("YouTube download failed for task {}: {}", taskId, result.getError());
                throw new ServiceException("Download failed: " + result.getError(), "YOUTUBE_DOWNLOAD_FAILED");
            }
            
        } catch (Exception e) {
            log.error("Error processing YouTube download for task {}: {}", taskId, e.getMessage(), e);
            throw new ServiceException("Error processing download: " + e.getMessage(), "YOUTUBE_PROCESS_ERROR");
        }
    }
    
    @Override
    public boolean supports(String serviceName) {
        return "youtube".equalsIgnoreCase(serviceName);
    }
    
    @Override
    public String getServiceName() {
        return "youtube";
    }
    
    @Override
    public String getOutputTemplate() {
        return "%(title)s [%(id)s].%(ext)s";
    }
    
    @Override
    public String getYtDlpOptions() {
        return "--no-playlist --no-part --embed-thumbnail --embed-metadata";
    }
    
    @Override
    public void validateFormat(String formatId) throws ServiceException {
        if (formatId == null || formatId.trim().isEmpty()) {
            throw new ServiceException("Format ID is required", "EMPTY_FORMAT_ID");
        }
        
        // Add YouTube-specific format validation if needed
    }
}