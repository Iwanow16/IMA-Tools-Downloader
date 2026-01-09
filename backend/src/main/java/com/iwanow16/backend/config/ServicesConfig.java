package com.iwanow16.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;

/**
 * Configuration for managing service enable/disable status
 */
@Configuration
@ConfigurationProperties(prefix = "services")
public class ServicesConfig {
    
    private static final Logger log = LoggerFactory.getLogger(ServicesConfig.class);
    private Youtube youtube = new Youtube();
    private Bilibili bilibili = new Bilibili();

    @PostConstruct
    public void init() {
        log.info("ServicesConfig initialized");
    }

    public Youtube getYoutube() {
        return youtube;
    }

    public void setYoutube(Youtube youtube) {
        this.youtube = youtube;
    }

    public Bilibili getBilibili() {
        return bilibili;
    }

    public void setBilibili(Bilibili bilibili) {
        this.bilibili = bilibili;
    }

    public boolean isServiceEnabled(String serviceName) {
        boolean enabled = false;
        switch (serviceName.toLowerCase()) {
            case "youtube" -> enabled = youtube.isEnabled();
            case "bilibili" -> enabled = bilibili.isEnabled();
        }
        return enabled;
    }

    // Inner class for YouTube service configuration
    public static class Youtube {
        private boolean enabled = false;
        private String description = "YouTube video downloader";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    // Inner class for Bilibili service configuration
    public static class Bilibili {
        private boolean enabled = false;
        private String description = "Bilibili video downloader";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
