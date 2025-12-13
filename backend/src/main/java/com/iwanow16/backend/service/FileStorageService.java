package com.iwanow16.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileStorageService {

    @Value("${app.output-dir:/app/downloads}")
    private String outputDir;

    public Path getFilePath(String filename) {
        return Paths.get(outputDir).resolve(filename).toAbsolutePath().normalize();
    }

    public boolean fileExists(String filename) {
        return Files.exists(getFilePath(filename));
    }

    public void ensureDirectories() throws Exception {
        Path p = Paths.get(outputDir);
        if (!Files.exists(p)) Files.createDirectories(p);
    }

    public File getFile(String filename) {
        return getFilePath(filename).toFile();
    }

    public Path getStorageDir() {
        return Paths.get(outputDir).toAbsolutePath().normalize();
    }
}
