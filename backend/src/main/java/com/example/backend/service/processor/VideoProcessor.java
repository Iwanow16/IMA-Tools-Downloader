package com.example.backend.service.processor;

import com.example.backend.model.dto.TaskStatusDto;
import com.example.backend.exception.ServiceException;

public interface VideoProcessor {
    
    /**
     * Process video download
     */
    TaskStatusDto processDownload(String url, String formatId, String quality, 
                                 String outputPath, String taskId) throws ServiceException;
    
    /**
     * Check if this processor supports the given service
     */
    boolean supports(String serviceName);
    
    /**
     * Get service name
     */
    String getServiceName();
    
    /**
     * Get default output template for this service
     */
    String getOutputTemplate();
    
    /**
     * Get default yt-dlp options for this service
     */
    String getYtDlpOptions();
    
    /**
     * Validate format ID for this service
     */
    void validateFormat(String formatId) throws ServiceException;
}