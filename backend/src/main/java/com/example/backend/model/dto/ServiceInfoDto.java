package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceInfoDto {
    private String id;
    private String name;
    private String displayName;
    private String description;
    private String iconUrl;
    private List<String> supportedDomains;
    private List<String> supportedFormats;
    private Boolean enabled;
    private Integer maxDuration; // in minutes
    private Long maxSize; // in MB
    private String configPath;
}