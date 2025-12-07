package com.example.backend.model.enums;

import lombok.Getter;

@Getter
public enum ServiceTypeEnum {
    YOUTUBE("youtube", "YouTube", "youtube.com,youtu.be"),
    GENERIC("generic", "Generic Video Service", "");
    
    private final String id;
    private final String displayName;
    private final String domains;
    
    ServiceTypeEnum(String id, String displayName, String domains) {
        this.id = id;
        this.displayName = displayName;
        this.domains = domains;
    }
    
    public static ServiceTypeEnum fromUrl(String url) {
        for (ServiceTypeEnum service : values()) {
            if (service.domains.isEmpty()) continue;
            
            String[] domainList = service.domains.split(",");
            for (String domain : domainList) {
                if (url.contains(domain.trim())) {
                    return service;
                }
            }
        }
        return GENERIC;
    }
    
    public static ServiceTypeEnum fromId(String id) {
        for (ServiceTypeEnum service : values()) {
            if (service.id.equalsIgnoreCase(id)) {
                return service;
            }
        }
        return GENERIC;
    }
}