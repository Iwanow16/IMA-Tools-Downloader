package com.example.backend.service.extractor;

import com.example.backend.model.dto.VideoInfoDto;
import com.example.backend.exception.ServiceException;

public interface VideoExtractor {
    
    /**
     * Get information about video from URL
     */
    VideoInfoDto extractInfo(String url) throws ServiceException;
    
    /**
     * Check if this extractor supports the given URL
     */
    boolean supports(String url);
    
    /**
     * Get service name
     */
    String getServiceName();
    
    /**
     * Get service display name
     */
    String getServiceDisplayName();
    
    /**
     * Validate URL for this service
     */
    void validateUrl(String url) throws ServiceException;
    
    /**
     * Get list of supported domains
     */
    String[] getSupportedDomains();
}