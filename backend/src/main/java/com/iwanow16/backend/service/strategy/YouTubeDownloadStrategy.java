package com.iwanow16.backend.service.strategy;

import com.iwanow16.backend.util.ProcessExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Стратегия скачивания видео с YouTube с поддержкой cookies и JS runtime.
 */
@Component
public class YouTubeDownloadStrategy implements DownloadStrategy {
    private static final Logger log = LoggerFactory.getLogger(YouTubeDownloadStrategy.class);

    @Value("${app.youtube.cookies-file:}")
    private String cookiesFile;

    @Value("${app.youtube.js-runtime:node}")
    private String jsRuntime;

    @Override
    public boolean supports(String url) {
        return url != null && (url.contains("youtube.com") || url.contains("youtu.be"));
    }

    @Override
    public String getServiceName() {
        return "youtube";
    }

    @Override
    public Path download(String url, Path outputDir, String formatId, String taskId) throws Exception {
        log.info("[{}] Downloading from YouTube: {}", taskId, url);
        
        List<String> cmd = new ArrayList<>();
        cmd.add("yt-dlp");

        // Добавить cookies, если они настроены
        if (cookiesFile != null && !cookiesFile.isBlank()) {
            cmd.add("--cookies");
            cmd.add(cookiesFile);
        }

        // Указать формат (если не задан, yt-dlp выберет лучший)
        if (formatId != null && !formatId.isBlank()) {
            cmd.add("-f");
            cmd.add(formatId);
        } else {
            // По умолчанию: лучшее видео + аудио (объединенные)
            cmd.add("-f");
            cmd.add("best[ext=mp4]/best");
        }

        // Продолжить неполные загрузки
        cmd.add("-c");

        // Указать шаблон имени файла
        String outputTemplate = "%(id)s.%(ext)s";
        cmd.add("-o");
        cmd.add(outputDir.resolve(outputTemplate).toString());

        // Добавить URL в конец команды
        cmd.add(url);

        log.info("[{}] Running command: {}", taskId, cmd);
        
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(outputDir.toFile());
        pb.redirectErrorStream(false);
        Process p = pb.start();

        // Читать output и error потоки
        StringBuilder output = new StringBuilder();
        StringBuilder error = new StringBuilder();
        
        Thread outputThread = new Thread(() -> {
            try (var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.debug("[{}] {}", taskId, line);
                }
            } catch (Exception e) {
                log.warn("[{}] Error reading output", taskId, e);
            }
        });

        Thread errorThread = new Thread(() -> {
            try (var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    error.append(line).append("\n");
                    log.debug("[{}] ERROR: {}", taskId, line);
                }
            } catch (Exception e) {
                log.warn("[{}] Error reading error stream", taskId, e);
            }
        });

        outputThread.start();
        errorThread.start();

        int rc = p.waitFor();
        outputThread.join(5000);
        errorThread.join(5000);

        if (rc != 0) {
            String errorMsg = error.toString().isEmpty() ? output.toString() : error.toString();
            log.error("[{}] YouTube download failed with code {}: {}", taskId, rc, errorMsg);
            throw new RuntimeException("YouTube download failed: " + errorMsg);
        }

        // Найти скачанный файл
        String videoId = extractVideoId(url);
        if (videoId == null) {
            throw new RuntimeException("Could not extract video ID from URL");
        }

        // Попытаться найти файл с расширением mp4, webm или другим видео форматом
        for (String ext : new String[]{"mp4", "mkv", "webm", "flv", "avi", "mov", "m4a", "aac", "opus"}) {
            Path file = outputDir.resolve(videoId + "." + ext);
            if (file.toFile().exists()) {
                log.info("[{}] Download completed: {}", taskId, file);
                return file;
            }
        }

        throw new RuntimeException("Downloaded file not found in output directory");
    }

    /**
     * Извлечь ID видео из URL YouTube.
     */
    private String extractVideoId(String url) {
        // youtube.com/watch?v=VIDEO_ID
        if (url.contains("watch?v=")) {
            int start = url.indexOf("watch?v=") + 8;
            int end = url.indexOf("&", start);
            if (end == -1) {
                end = url.length();
            }
            return url.substring(start, end);
        }

        // youtu.be/VIDEO_ID
        if (url.contains("youtu.be/")) {
            int start = url.indexOf("youtu.be/") + 9;
            int end = url.indexOf("?", start);
            if (end == -1) {
                end = url.length();
            }
            return url.substring(start, end);
        }

        return null;
    }
}
