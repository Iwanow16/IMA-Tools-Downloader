package com.example.backend.controller;

import com.example.backend.model.dto.ApiResponseDto;
import com.example.backend.model.dto.ServiceInfoDto;
import com.example.backend.service.extractor.VideoExtractor;
import com.example.backend.service.extractor.VideoExtractorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/services")
@Slf4j
@CrossOrigin(origins = "*")
public class ServiceController {
    
    @Autowired
    private VideoExtractorFactory extractorFactory;
    
    @GetMapping
    public ResponseEntity<ApiResponseDto<List<ServiceInfoDto>>> getSupportedServices() {
        try {
            Map<String, VideoExtractor> extractors = extractorFactory.getAllExtractors();
            
            List<ServiceInfoDto> services = extractors.values().stream()
                .map(extractor -> ServiceInfoDto.builder()
                    .id(extractor.getServiceName())
                    .name(extractor.getServiceName())
                    .displayName(extractor.getServiceDisplayName())
                    .supportedDomains(List.of(extractor.getSupportedDomains()))
                    .enabled(true)
                    .build())
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponseDto.success(services));
            
        } catch (Exception e) {
            log.error("Error getting supported services: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDto.error(
                        "Failed to get supported services",
                        "SERVICE_LIST_ERROR",
                        500
                    ));
        }
    }
    
    @GetMapping("/{serviceName}")
    public ResponseEntity<ApiResponseDto<ServiceInfoDto>> getServiceInfo(
            @PathVariable String serviceName) {
        
        try {
            if (!extractorFactory.isServiceSupported(serviceName)) {
                return ResponseEntity.notFound().build();
            }
            
            VideoExtractor extractor = extractorFactory.getExtractor(serviceName);
            
            ServiceInfoDto serviceInfo = ServiceInfoDto.builder()
                .id(extractor.getServiceName())
                .name(extractor.getServiceName())
                .displayName(extractor.getServiceDisplayName())
                .supportedDomains(List.of(extractor.getSupportedDomains()))
                .enabled(true)
                .build();
            
            return ResponseEntity.ok(ApiResponseDto.success(serviceInfo));
            
        } catch (Exception e) {
            log.error("Error getting service info for {}: {}", serviceName, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDto.error(
                        "Failed to get service info",
                        "SERVICE_INFO_ERROR",
                        500
                    ));
        }
    }
    
    @GetMapping("/detect")
    public ResponseEntity<ApiResponseDto<ServiceInfoDto>> detectService(
            @RequestParam String url) {
        
        try {
            VideoExtractor extractor = extractorFactory.getExtractorForUrl(url);
            
            ServiceInfoDto serviceInfo = ServiceInfoDto.builder()
                .id(extractor.getServiceName())
                .name(extractor.getServiceName())
                .displayName(extractor.getServiceDisplayName())
                .supportedDomains(List.of(extractor.getSupportedDomains()))
                .enabled(true)
                .build();
            
            return ResponseEntity.ok(ApiResponseDto.success("Service detected", serviceInfo));
            
        } catch (Exception e) {
            log.error("Error detecting service for URL {}: {}", url, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error(
                        "Failed to detect service: " + e.getMessage(),
                        "SERVICE_DETECTION_ERROR",
                        400
                    ));
        }
    }
}