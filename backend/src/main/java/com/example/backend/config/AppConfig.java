package com.example.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppConfig {
    
    private String version = "1.0.0";
    private String environment = "dev";
    private String tempDir = "/app/temp";
    private String downloadDir = "/app/downloads";
    private String logDir = "/app/logs";
    private String configDir = "/app/config";
    
    @Bean
    public Map<String, Map<String, Object>> serviceConfigs() throws IOException {
        Map<String, Map<String, Object>> configs = new HashMap<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:services/*-config.yml");
        
        Yaml yaml = new Yaml();
        for (Resource resource : resources) {
            try (InputStream inputStream = resource.getInputStream()) {
                Map<String, Object> config = yaml.load(inputStream);
                String serviceName = resource.getFilename()
                    .replace("-config.yml", "")
                    .toLowerCase();
                configs.put(serviceName, config);
            }
        }
        
        return configs;
    }
}