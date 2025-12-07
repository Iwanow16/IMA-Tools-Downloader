package com.example.backend.controller;

import com.example.backend.model.dto.ApiResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@Slf4j
public class HealthController {
    
    @Value("${app.version:1.0.0}")
    private String appVersion;
    
    @Value("${app.environment:dev}")
    private String environment;
    
    @GetMapping
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("version", appVersion);
        health.put("environment", environment);
        health.put("timestamp", System.currentTimeMillis());
        
        // Add system info
        Runtime runtime = Runtime.getRuntime();
        health.put("memory", Map.of(
            "free", runtime.freeMemory(),
            "total", runtime.totalMemory(),
            "max", runtime.maxMemory(),
            "used", runtime.totalMemory() - runtime.freeMemory()
        ));
        
        return ResponseEntity.ok(ApiResponseDto.success(health));
    }
    
    @GetMapping("/ready")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> readinessCheck() {
        Map<String, Object> readiness = new HashMap<>();
        readiness.put("status", "READY");
        readiness.put("timestamp", System.currentTimeMillis());
        
        // Add service readiness checks here
        // For example: database connectivity, external service dependencies
        
        return ResponseEntity.ok(ApiResponseDto.success(readiness));
    }
    
    @GetMapping("/live")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> livenessCheck() {
        Map<String, Object> liveness = new HashMap<>();
        liveness.put("status", "LIVE");
        liveness.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(ApiResponseDto.success(liveness));
    }
}