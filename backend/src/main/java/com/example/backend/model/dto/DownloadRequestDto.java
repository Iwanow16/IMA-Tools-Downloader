package com.example.backend.model.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
public class DownloadRequestDto {
    
    @NotBlank(message = "URL is required")
    private String url;
    
    private String formatId;
    private String quality;
    private Boolean audioOnly = false;
    private String serviceName;
    
    @Pattern(regexp = "^[a-zA-Z0-9-_\\s\\.]+$", message = "Invalid filename format")
    private String customFilename;
    
    public String getServiceName() {
        if (serviceName != null && !serviceName.isEmpty()) {
            return serviceName.toLowerCase();
        }
        
        // Auto-detect service from URL
        if (url.contains("youtube.com") || url.contains("youtu.be")) {
            return "youtube";
        } else if (url.contains("vimeo.com")) {
            return "vimeo";
        } else if (url.contains("tiktok.com")) {
            return "tiktok";
        } else if (url.contains("instagram.com")) {
            return "instagram";
        } else if (url.contains("twitter.com") || url.contains("x.com")) {
            return "twitter";
        }
        
        return "generic";
    }
}