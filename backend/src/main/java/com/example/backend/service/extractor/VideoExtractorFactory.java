package com.example.backend.service.extractor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
public class VideoExtractorFactory {
    
    @Autowired
    private ApplicationContext applicationContext;
    
    private final Map<String, VideoExtractor> extractors = new HashMap<>();
    
    @PostConstruct
    public void init() {
        // Auto-discover all VideoExtractor implementations
        Map<String, VideoExtractor> beans = applicationContext.getBeansOfType(VideoExtractor.class);
        beans.values().forEach(extractor -> {
            extractors.put(extractor.getServiceName().toLowerCase(), extractor);
        });
    }
    
    public VideoExtractor getExtractor(String serviceName) {
        VideoExtractor extractor = extractors.get(serviceName.toLowerCase());
        if (extractor == null) {
            throw new IllegalArgumentException("No extractor found for service: " + serviceName);
        }
        return extractor;
    }
    
    public VideoExtractor getExtractorForUrl(String url) {
        for (VideoExtractor extractor : extractors.values()) {
            if (extractor.supports(url)) {
                return extractor;
            }
        }
        // Return generic extractor if no specific one found
        return extractors.get("generic");
    }
    
    public Map<String, VideoExtractor> getAllExtractors() {
        return new HashMap<>(extractors);
    }
    
    public boolean isServiceSupported(String serviceName) {
        return extractors.containsKey(serviceName.toLowerCase());
    }
}