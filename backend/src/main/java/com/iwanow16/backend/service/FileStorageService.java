package com.iwanow16.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${app.output-dir:/app/downloads}")
    private String outputDir;

    public Path getFilePath(String filename) {
        Path result = Paths.get(outputDir).resolve(filename).toAbsolutePath().normalize();
        log.debug("📁 Resolved file path | Filename: {} | Path: {}", filename, result);
        return result;
    }

    public boolean fileExists(String filename) {
        boolean exists = Files.exists(getFilePath(filename));
        log.debug("🔍 File check | Filename: {} | Exists: {}", filename, exists);
        return exists;
    }

    public void ensureDirectories() throws Exception {
        Path p = Paths.get(outputDir);
        if (!Files.exists(p)) {
            Files.createDirectories(p);
            log.info("📂 Created storage directory | Path: {}", p.toAbsolutePath());
        } else {
            log.debug("📂 Storage directory exists | Path: {}", p.toAbsolutePath());
        }
    }

    public File getFile(String filename) {
        File result = getFilePath(filename).toFile();
        log.debug("📄 Getting file object | Filename: {} | AbsolutePath: {}", filename, result.getAbsolutePath());
        return result;
    }

    public Path getStorageDir() {
        Path result = Paths.get(outputDir).toAbsolutePath().normalize();
        log.debug("📦 Storage directory: {}", result);
        return result;
    }
}
