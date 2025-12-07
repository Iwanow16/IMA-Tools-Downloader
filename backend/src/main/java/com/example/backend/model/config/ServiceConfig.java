package com.example.backend.model.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceConfig {
    private String id;
    private String name;
    private String displayName;
    private String description;
    private String icon;
    private Boolean enabled;
    private List<String> domains;
    private List<String> formats;
    private Integer maxDuration; // minutes
    private Long maxSize; // MB
    private String extractorClass;
    private String processorClass;
    private String ytDlpOptions;
    private String outputTemplate;
    private Boolean supportsPlaylist;
    private Boolean supportsSubtitles;
    private Boolean supportsThumbnail;
    private Integer rateLimitPerHour;
}