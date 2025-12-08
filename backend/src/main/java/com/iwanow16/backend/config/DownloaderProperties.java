package com.iwanow16.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "downloader")
public class DownloaderProperties {
    private int maxConcurrentDownloads = 3;
    private int maxConcurrentPerIp = 2;
    private int maxDurationMinutes = 120;
    private int maxSizeMb = 2048;
    private List<String> allowedDomains;

    public int getMaxConcurrentDownloads() {
        return maxConcurrentDownloads;
    }

    public void setMaxConcurrentDownloads(int maxConcurrentDownloads) {
        this.maxConcurrentDownloads = maxConcurrentDownloads;
    }

    public int getMaxConcurrentPerIp() {
        return maxConcurrentPerIp;
    }

    public void setMaxConcurrentPerIp(int maxConcurrentPerIp) {
        this.maxConcurrentPerIp = maxConcurrentPerIp;
    }

    public int getMaxDurationMinutes() {
        return maxDurationMinutes;
    }

    public void setMaxDurationMinutes(int maxDurationMinutes) {
        this.maxDurationMinutes = maxDurationMinutes;
    }

    public int getMaxSizeMb() {
        return maxSizeMb;
    }

    public void setMaxSizeMb(int maxSizeMb) {
        this.maxSizeMb = maxSizeMb;
    }

    public List<String> getAllowedDomains() {
        return allowedDomains;
    }

    public void setAllowedDomains(List<String> allowedDomains) {
        this.allowedDomains = allowedDomains;
    }
}
