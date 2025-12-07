package com.example.backend.service.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
public class VideoProcessorFactory {
    
    @Autowired
    private ApplicationContext applicationContext;
    
    private final Map<String, VideoProcessor> processors = new HashMap<>();
    
    @PostConstruct
    public void init() {
        // Auto-discover all VideoProcessor implementations
        Map<String, VideoProcessor> beans = applicationContext.getBeansOfType(VideoProcessor.class);
        beans.values().forEach(processor -> {
            processors.put(processor.getServiceName().toLowerCase(), processor);
        });
    }
    
    public VideoProcessor getProcessor(String serviceName) {
        VideoProcessor processor = processors.get(serviceName.toLowerCase());
        if (processor == null) {
            throw new IllegalArgumentException("No processor found for service: " + serviceName);
        }
        return processor;
    }
    
    public Map<String, VideoProcessor> getAllProcessors() {
        return new HashMap<>(processors);
    }
    
    public boolean isServiceSupported(String serviceName) {
        return processors.containsKey(serviceName.toLowerCase());
    }
}
