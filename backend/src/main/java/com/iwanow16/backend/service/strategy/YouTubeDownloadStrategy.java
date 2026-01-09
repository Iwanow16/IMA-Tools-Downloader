package com.iwanow16.backend.service.strategy;

import com.iwanow16.backend.util.MediaMerger;
import com.iwanow16.backend.util.FrameExtractorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Стратегия скачивания видео с YouTube с поддержкой cookies и JS runtime.
 */
@Component
public class YouTubeDownloadStrategy implements DownloadStrategy {
    private static final Logger log = LoggerFactory.getLogger(YouTubeDownloadStrategy.class);
    
    // Patterns for parsing progress from yt-dlp
    private static final Pattern PROGRESS_PATTERN = Pattern.compile("\\[download\\] (\\d+(?:\\.\\d+)?)%");
    private static final Pattern SPEED_PATTERN = Pattern.compile("at\\s+(\\d+(?:\\.\\d+)?[KMGT]?B/s)");
    private static final Pattern ETA_PATTERN = Pattern.compile("ETA\\s+(\\d+):(\\d+)");

    @Value("${app.youtube.cookies-file:}")
    private String cookiesFile;

    @Value("${app.youtube.js-runtime:node}")
    private String jsRuntime;

    @Value("${app.youtube.use-remote-components:true}")
    private boolean useRemoteComponents;

    @Autowired
    private FrameExtractorUtil frameExtractorUtil;
    
    // Callback для обновления прогресса
    private BiConsumer<String, java.util.Map<String, Object>> progressCallback;

    public void setProgressCallback(BiConsumer<String, java.util.Map<String, Object>> callback) {
        this.progressCallback = callback;
    }

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
        log.info("📹 YouTube download started | TaskID: {} | URL: {} | Format: {}", taskId, url, formatId);
        long startTime = System.currentTimeMillis();
        
        List<String> cmd = new ArrayList<>();
        cmd.add("yt-dlp");
        
        // Не скачивать весь плейлист, даже если URL содержит параметры плейлиста
        cmd.add("--no-playlist");
        
        // Добавить JS runtime для YouTube (требуется для новых версий)
        cmd.add("--js-runtimes");
        cmd.add("node");
        // Добавить remote components для решения JS challenges
        if (useRemoteComponents) {
            cmd.add("--remote-components");
            cmd.add("ejs:github");
            log.debug("📡 Remote EJS components enabled | TaskID: {}", taskId);
        }
        // Добавить cookies, если они настроены
        if (cookiesFile != null && !cookiesFile.isBlank()) {
            cmd.add("--cookies");
            cmd.add(cookiesFile);
            log.debug("🍪 Using cookies file | TaskID: {}", taskId);
        }

        // Указать формат (если не задан, yt-dlp выберет лучший)
        if (formatId != null && !formatId.isBlank()) {
            cmd.add("-f");
            // Если это синтетический формат (video_id+audio_id), использовать как есть
            // иначе добавить лучшее аудио
            if (formatId.contains("+")) {
                cmd.add(formatId);
                log.info("🔀 Using synthetic format (video+audio combination): {}", formatId);
            } else {
                cmd.add(formatId);
            }
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

        log.debug("⏳ Executing yt-dlp command | TaskID: {} | Format: {}", taskId, formatId);
        
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
                    parseAndUpdateProgress(taskId, line);
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
                    parseAndUpdateProgress(taskId, line);
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
            log.error("❌ YouTube download failed | TaskID: {} | Code: {} | Duration: {}ms | Error: {}", 
                    taskId, rc, duration, errorMsg);
            throw new RuntimeException("YouTube download failed: " + errorMsg);
        }

        // Найти скачанный файл
        String videoId = extractVideoId(url);
        if (videoId == null) {
            log.error("❌ Could not extract video ID | TaskID: {} | URL: {}", taskId, url);
            throw new RuntimeException("Could not extract video ID from URL");
        }

        // Ищем видео и аудио файлы отдельно
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
        for (String ext : new String[]{"m4a", "aac", "mp3", "opus", "wav", "wma"}) {
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
                log.info("✅ YouTube download completed (merged) | TaskID: {} | Filename: {} | Duration: {}ms", 
                        taskId, mergedFile.getFileName(), duration);
                return mergedFile;
            } catch (Exception e) {
                log.warn("⚠️ Failed to merge with ffmpeg, returning video file only | TaskID: {} | Error: {}", 
                        taskId, e.getMessage());
                long duration = System.currentTimeMillis() - startTime;
                log.info("✅ YouTube download completed (video only) | TaskID: {} | Filename: {} | Duration: {}ms", 
                        taskId, videoFile.getFileName(), duration);
                return videoFile;
            }
        }
        
        // Если есть только видео
        if (videoFile != null) {
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ YouTube download completed | TaskID: {} | Filename: {} | Duration: {}ms", 
                    taskId, videoFile.getFileName(), duration);
            return videoFile;
        }
        
        // Если есть только аудио
        if (audioFile != null) {
            log.warn("⚠️ Only audio file found, returning audio | TaskID: {}", taskId);
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ YouTube download completed (audio only) | TaskID: {} | Filename: {} | Duration: {}ms", 
                    taskId, audioFile.getFileName(), duration);
            return audioFile;
        }

        long duration = System.currentTimeMillis() - startTime;
        log.error("❌ Downloaded file not found | TaskID: {} | VideoID: {} | Duration: {}ms", 
                taskId, videoId, duration);
        throw new RuntimeException("Downloaded file not found in output directory");
    }

    @Override
    public Path downloadTimeRange(String url, Path outputDir, String formatId, String taskId,
                                  String startTime, String endTime) throws Exception {
        log.info("⏱️  YouTube download with time range | TaskID: {} | Start: {}s | End: {}s", 
                taskId, startTime, endTime);
        
        // Сначала скачиваем полное видео
        Path fullVideoPath = download(url, outputDir, formatId, taskId);
        
        // Затем вырезаем нужный диапазон
        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-i");
        cmd.add(fullVideoPath.toString());
        cmd.add("-ss");
        cmd.add(startTime);
        cmd.add("-to");
        cmd.add(endTime);
        cmd.add("-c");
        cmd.add("copy");  // Копируем без перекодирования для скорости
        
        // Генерируем имя выходного файла
        String filename = "trimmed_" + System.currentTimeMillis() + ".mp4";
        Path outputPath = outputDir.resolve(filename);
        cmd.add(outputPath.toString());

        log.debug("⏳ Executing ffmpeg trim command | TaskID: {}", taskId);
        
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        StringBuilder output = new StringBuilder();
        try (var reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.debug("📊 ffmpeg output | TaskID: {} | {}", taskId, line);
            }
        }

        int exitCode = p.waitFor();
        
        if (exitCode != 0) {
            log.error("❌ Time range extraction failed | TaskID: {} | Exit code: {}", taskId, exitCode);
            throw new RuntimeException("Failed to extract time range");
        }

        log.info("✅ Time range extraction completed | TaskID: {} | File: {}", taskId, filename);
        return outputPath;
    }

    @Override
    public Path extractFrame(String url, Path outputDir, String taskId, String frameTime) throws Exception {
        log.info("📷 YouTube frame extraction | TaskID: {} | Frame time: {}s", taskId, frameTime);
        
        // Сначала загрузим видео в формате, с которым может работать ffmpeg
        // Используем лучший доступный формат (обычно 720p или выше)
        String tempVideoFile = "temp_" + System.currentTimeMillis() + ".mp4";
        Path tempVideoPath = outputDir.resolve(tempVideoFile);

        log.debug("⏳ Downloading video for frame extraction | TaskID: {} | Temp file: {}", 
                taskId, tempVideoFile);

        // Используем yt-dlp для загрузки лучшего видеоформата
        List<String> downloadCmd = new ArrayList<>();
        downloadCmd.add("yt-dlp");
        
        // Не скачивать весь плейлист, даже если URL содержит параметры плейлиста
        downloadCmd.add("--no-playlist");
        
        // Добавить JS runtime для YouTube (требуется для новых версий)
        downloadCmd.add("--js-runtimes");
        downloadCmd.add(jsRuntime);

        // Добавить remote components для решения JS challenges
        if (useRemoteComponents) {
            downloadCmd.add("--remote-components");
            downloadCmd.add("ejs:github");
            log.debug("📡 Remote EJS components enabled | TaskID: {}", taskId);
        }
        
        // Добавить cookies, если они настроены
        if (cookiesFile != null && !cookiesFile.isBlank()) {
            downloadCmd.add("--cookies");
            downloadCmd.add(cookiesFile);
            log.debug("🍪 Using cookies file | TaskID: {}", taskId);
        }
        
        downloadCmd.add("-f");
        downloadCmd.add("b");  // Лучший доступный формат (без предупреждения)
        downloadCmd.add("-o");
        downloadCmd.add(tempVideoPath.toString());
        downloadCmd.add(url);

        log.debug("⏳ Executing yt-dlp download | TaskID: {}", taskId);
        ProcessBuilder pb = new ProcessBuilder(downloadCmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        StringBuilder output = new StringBuilder();
        try (var reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.debug("📥 Download output | TaskID: {} | {}", taskId, line);
            }
        }

        int exitCode = p.waitFor();
        if (exitCode != 0) {
            log.error("❌ Failed to download video for frame extraction | TaskID: {} | Error: {}", 
                    taskId, output);
            throw new RuntimeException("Failed to download video for frame extraction");
        }

        if (!java.nio.file.Files.exists(tempVideoPath)) {
            log.error("❌ Temp video file not created | TaskID: {}", taskId);
            throw new RuntimeException("Temp video file not created");
        }

        log.debug("✅ Video downloaded | TaskID: {} | File: {}", taskId, tempVideoFile);

        // Теперь извлекаем кадр из локального файла
        List<String> ffmpegCmd = new ArrayList<>();
        ffmpegCmd.add("ffmpeg");
        ffmpegCmd.add("-ss");
        ffmpegCmd.add(frameTime);
        ffmpegCmd.add("-i");
        ffmpegCmd.add(tempVideoPath.toString());
        ffmpegCmd.add("-vframes");
        ffmpegCmd.add("1");
        ffmpegCmd.add("-q:v");
        ffmpegCmd.add("2");  // Высокое качество
        ffmpegCmd.add("-f");
        ffmpegCmd.add("image2");

        String frameFilename = "frame_" + System.currentTimeMillis() + ".png";
        Path outputPath = outputDir.resolve(frameFilename);
        ffmpegCmd.add(outputPath.toString());

        log.debug("⏳ Extracting frame with ffmpeg | TaskID: {} | Output: {}", taskId, frameFilename);

        pb = new ProcessBuilder(ffmpegCmd);
        pb.redirectErrorStream(true);
        p = pb.start();

        output = new StringBuilder();
        try (var reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.debug("📊 ffmpeg output | TaskID: {} | {}", taskId, line);
            }
        }

        exitCode = p.waitFor();
        
        if (exitCode != 0) {
            log.error("❌ Frame extraction failed | TaskID: {} | Exit code: {} | Output: {}", 
                    taskId, exitCode, output);
            // Очистим временный файл перед выбросом ошибки
            try {
                java.nio.file.Files.deleteIfExists(tempVideoPath);
            } catch (Exception e) {
                log.warn("⚠️ Failed to delete temp video file | TaskID: {} | File: {}", 
                        taskId, tempVideoPath);
            }
            throw new RuntimeException("Failed to extract frame: " + output);
        }

        if (!java.nio.file.Files.exists(outputPath)) {
            log.error("❌ Frame file not created | TaskID: {} | Expected: {}", taskId, outputPath);
            try {
                java.nio.file.Files.deleteIfExists(tempVideoPath);
            } catch (Exception e) {
                log.warn("⚠️ Failed to delete temp video file | TaskID: {} | File: {}", 
                        taskId, tempVideoPath);
            }
            throw new RuntimeException("Frame extraction failed: output file not created");
        }

        // Удаляем временный файл
        try {
            java.nio.file.Files.deleteIfExists(tempVideoPath);
            log.debug("🗑️ Temp video file deleted | TaskID: {} | File: {}", taskId, tempVideoFile);
        } catch (Exception e) {
            log.warn("⚠️ Failed to delete temp video file | TaskID: {} | File: {}", 
                    taskId, tempVideoPath);
        }

        long fileSize = java.nio.file.Files.size(outputPath);
        log.info("✅ Frame extracted successfully | TaskID: {} | File: {} | Size: {} bytes", 
                taskId, frameFilename, fileSize);
        return outputPath;
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

    /**
     * Parse progress from yt-dlp output and call callback
     */
    private void parseAndUpdateProgress(String taskId, String line) {
        if (progressCallback == null) {
            return;
        }
        
        try {
            // Parse progress: [download] 45.3%
            Matcher progressMatcher = PROGRESS_PATTERN.matcher(line);
            if (progressMatcher.find()) {
                double percent = Double.parseDouble(progressMatcher.group(1));
                int progress = (int) percent;
                
                // Parse speed: at 5.23MB/s
                String speed = null;
                Matcher speedMatcher = SPEED_PATTERN.matcher(line);
                if (speedMatcher.find()) {
                    speed = speedMatcher.group(1);
                }
                
                // Parse ETA: ETA 00:45
                Integer eta = null;
                Matcher etaMatcher = ETA_PATTERN.matcher(line);
                if (etaMatcher.find()) {
                    int minutes = Integer.parseInt(etaMatcher.group(1));
                    int seconds = Integer.parseInt(etaMatcher.group(2));
                    eta = minutes * 60 + seconds;
                }
                
                java.util.Map<String, Object> progressData = new java.util.HashMap<>();
                progressData.put("progress", progress);
                progressData.put("speed", speed);
                progressData.put("eta", eta);
                
                progressCallback.accept(taskId, progressData);
            }
        } catch (Exception e) {
            log.debug("Failed to parse progress from line: {}", line, e);
        }
    }}