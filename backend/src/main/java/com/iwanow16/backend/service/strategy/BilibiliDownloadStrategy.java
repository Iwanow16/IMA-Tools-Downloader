package com.iwanow16.backend.service.strategy;

import com.iwanow16.backend.util.MediaMerger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Стратегия скачивания видео с Bilibili.
 */
@Component
public class BilibiliDownloadStrategy implements DownloadStrategy {
    private static final Logger log = LoggerFactory.getLogger(BilibiliDownloadStrategy.class);

    @Override
    public boolean supports(String url) {
        return url != null && (url.contains("bilibili.com") || url.contains("b23.tv"));
    }

    @Override
    public String getServiceName() {
        return "bilibili";
    }

    @Override
    public Path download(String url, Path outputDir, String formatId, String taskId) throws Exception {
        log.info("🎬 Bilibili download started | TaskID: {} | URL: {}", taskId, url);
        long startTime = System.currentTimeMillis();
        
        String cookiesPath = "/app/resources/bilibili_cookies.txt";
        
        // Построить команду yt-dlp для Bilibili
        java.util.List<String> cmd = new java.util.ArrayList<>();
        cmd.add("yt-dlp");
        cmd.add("--user-agent");
        cmd.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        cmd.add("--cookies");
        cmd.add(cookiesPath);
        cmd.add("--no-check-certificate");
        cmd.add("--socket-timeout");
        cmd.add("30");
        cmd.add("--retries");
        cmd.add("3");
        cmd.add("--fragment-retries");
        cmd.add("3");
        cmd.add("--extractor-args");
        cmd.add("bilibili:is_story=False");
        cmd.add("--extractor-args");
        cmd.add("bilibili:metadata_api=true");
        
        // Указать формат
        if (formatId != null && !formatId.isBlank()) {
            // Проверить если это синтетический формат (video_id+audio_id)
            if (formatId.contains("+")) {
                cmd.add("-f");
                cmd.add(formatId);
                log.info("🔀 Using synthetic format (video+audio combination): {}", formatId);
            } else {
                // Для одиночного формата, попробовать добавить лучшее аудио
                cmd.add("-f");
                cmd.add(formatId + "+bestaudio[ext=m4a]/best");
            }
        } else {
            // Лучший формат по умолчанию
            cmd.add("-f");
            cmd.add("best[ext=mp4]/best");
        }
        
        // Опции
        cmd.add("-c"); // Continue on errors
        cmd.add("-o");
        cmd.add(outputDir.resolve("%(id)s.%(ext)s").toString());
        cmd.add(url);
        
        log.debug("⏳ Executing yt-dlp for Bilibili | TaskID: {} | URL: {}", taskId, url);
        
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
                    log.debug("📊 yt-dlp output | TaskID: {} | {}", taskId, line);
                }
            } catch (Exception e) {
                log.warn("⚠️ Error reading output | TaskID: {}", taskId, e);
            }
        });
        
        Thread errorThread = new Thread(() -> {
            try (var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    error.append(line).append("\n");
                    log.debug("⚠️ yt-dlp error | TaskID: {} | {}", taskId, line);
                }
            } catch (Exception e) {
                log.warn("⚠️ Error reading error stream | TaskID: {}", taskId, e);
            }
        });
        
        outputThread.start();
        errorThread.start();
        
        int rc = p.waitFor();
        outputThread.join(5000);
        errorThread.join(5000);
        
        if (rc != 0) {
            String errorMsg = error.toString().isEmpty() ? output.toString() : error.toString();
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ Bilibili download failed | TaskID: {} | Code: {} | Duration: {}ms | Error: {}", 
                    taskId, rc, duration, errorMsg);
            throw new RuntimeException("Bilibili download failed: " + errorMsg);
        }
        
        // Получить ID видео из URL
        String videoId = extractVideoId(url);
        if (videoId == null) {
            log.error("❌ Could not extract video ID | TaskID: {} | URL: {}", taskId, url);
            throw new RuntimeException("Could not extract video ID from URL");
        }

        // Попытаться найти скачанный файл (видео или аудио)
        Path videoFile = null;
        Path audioFile = null;
        
        // Сначала ищем файлы видео
        for (String ext : new String[]{"mp4", "mkv", "webm", "flv", "avi", "mov"}) {
            Path file = outputDir.resolve(videoId + "." + ext);
            if (file.toFile().exists()) {
                videoFile = file;
                log.debug("📹 Found video file: {}", file.getFileName());
                break;
            }
        }
        
        // Потом ищем файлы аудио
        for (String ext : new String[]{"m4a", "aac", "mp3", "opus", "wav"}) {
            Path file = outputDir.resolve(videoId + "." + ext);
            if (file.toFile().exists()) {
                audioFile = file;
                log.debug("🎵 Found audio file: {}", file.getFileName());
                break;
            }
        }
        
        // Если нашли оба файла, объединяем с помощью ffmpeg
        if (videoFile != null && audioFile != null) {
            log.info("🔀 Found separate video and audio files, merging with ffmpeg | TaskID: {}", taskId);
            try {
                Path mergedFile = outputDir.resolve(videoId + "_merged.mp4");
                MediaMerger.mergeVideoAudio(videoFile, audioFile, mergedFile, taskId);
                long duration = System.currentTimeMillis() - startTime;
                log.info("✅ Bilibili download completed (merged) | TaskID: {} | Filename: {} | Duration: {}ms", 
                        taskId, mergedFile.getFileName(), duration);
                return mergedFile;
            } catch (Exception e) {
                log.warn("⚠️ Failed to merge with ffmpeg, returning video file only | TaskID: {} | Error: {}", 
                        taskId, e.getMessage());
                long duration = System.currentTimeMillis() - startTime;
                log.info("✅ Bilibili download completed (video only) | TaskID: {} | Filename: {} | Duration: {}ms", 
                        taskId, videoFile.getFileName(), duration);
                return videoFile;
            }
        }
        
        // Если есть только видео
        if (videoFile != null) {
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Bilibili download completed | TaskID: {} | Filename: {} | Duration: {}ms", 
                    taskId, videoFile.getFileName(), duration);
            return videoFile;
        }
        
        // Если есть только аудио
        if (audioFile != null) {
            log.warn("⚠️ Only audio file found, returning audio | TaskID: {}", taskId);
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Bilibili download completed (audio only) | TaskID: {} | Filename: {} | Duration: {}ms", 
                    taskId, audioFile.getFileName(), duration);
            return audioFile;
        }

        long duration = System.currentTimeMillis() - startTime;
        log.error("❌ Downloaded file not found | TaskID: {} | VideoID: {} | Duration: {}ms", 
                taskId, videoId, duration);
        throw new RuntimeException("Downloaded file not found in output directory");
    }
    
    /**
     * Извлечь ID видео из URL Bilibili.
     */
    private String extractVideoId(String url) {
        // Примеры URL:
        // https://www.bilibili.com/video/BV1234567890/
        // https://b23.tv/BV1234567890
        
        if (url.contains("bilibili.com/video/")) {
            int start = url.indexOf("bilibili.com/video/") + 19;
            int end = url.indexOf("/", start);
            if (end == -1) {
                end = url.indexOf("?", start);
            }
            if (end == -1) {
                end = url.length();
            }
            return url.substring(start, end);
        }
        
        if (url.contains("b23.tv/")) {
            int start = url.indexOf("b23.tv/") + 7;
            int end = url.indexOf("?", start);
            if (end == -1) {
                end = url.length();
            }
            return url.substring(start, end);
        }
        
        return null;
    }
}
