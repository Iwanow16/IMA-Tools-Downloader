package com.iwanow16.backend.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Утилита для объединения видео и аудио потоков с помощью ffmpeg.
 */
public class MediaMerger {
    private static final Logger log = LoggerFactory.getLogger(MediaMerger.class);

    /**
     * Объединить видео и аудио файлы в один файл с помощью ffmpeg.
     * Поддерживает автоматическое преобразование в mp4 и управление кодеками.
     *
     * @param videoFile   путь к файлу видео
     * @param audioFile   путь к файлу аудио (может быть null)
     * @param outputFile  путь к выходному файлу
     * @param taskId      ID задачи для логирования
     * @return путь к выходному файлу, если успешно
     * @throws Exception если объединение не удалось
     */
    public static Path mergeVideoAudio(Path videoFile, Path audioFile, Path outputFile, String taskId) throws Exception {
        // Если нет аудиофайла, просто копируем видео
        if (audioFile == null || !Files.exists(audioFile)) {
            log.info("⚠️ No audio file provided, using video only | TaskID: {}", taskId);
            if (!Files.exists(videoFile)) {
                throw new RuntimeException("Video file not found: " + videoFile);
            }
            Files.copy(videoFile, outputFile);
            return outputFile;
        }

        // Если видеофайл не существует, копируем аудио
        if (!Files.exists(videoFile)) {
            log.info("⚠️ No video file found, using audio only | TaskID: {}", taskId);
            Files.copy(audioFile, outputFile);
            return outputFile;
        }

        log.info("🎬 Merging video and audio with ffmpeg | TaskID: {} | Video: {} | Audio: {}", 
                taskId, videoFile.getFileName(), audioFile.getFileName());
        long startTime = System.currentTimeMillis();

        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-i");
        cmd.add(videoFile.toString());
        cmd.add("-i");
        cmd.add(audioFile.toString());
        // Копировать кодеки без перекодирования (быстро)
        cmd.add("-c:v");
        cmd.add("copy");
        cmd.add("-c:a");
        cmd.add("copy");
        // Если видео и аудио не синхронизированы, привязать их
        cmd.add("-sync");
        cmd.add("1");
        // Перезаписать выходной файл без вопросов
        cmd.add("-y");
        // Выходной файл
        cmd.add(outputFile.toString());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        StringBuilder output = new StringBuilder();
        Thread outputThread = new Thread(() -> {
            try (var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    // Логировать только важные строки
                    if (line.contains("Duration") || line.contains("bitrate") || line.contains("speed")) {
                        log.debug("📊 ffmpeg | TaskID: {} | {}", taskId, line);
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ Error reading ffmpeg output | TaskID: {}", taskId, e);
            }
        });

        outputThread.start();

        // Ждём завершения с таймаутом 5 минут
        boolean finished = p.waitFor(5, TimeUnit.MINUTES);
        outputThread.join(5000);

        if (!finished) {
            p.destroyForcibly();
            log.error("❌ ffmpeg process timed out | TaskID: {}", taskId);
            throw new RuntimeException("ffmpeg merge operation timed out");
        }

        int rc = p.exitValue();
        if (rc != 0) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ ffmpeg merge failed | TaskID: {} | Code: {} | Duration: {}ms | Output: {}", 
                    taskId, rc, duration, output.toString());
            throw new RuntimeException("ffmpeg merge failed with code " + rc);
        }

        // Удалить исходные файлы видео и аудио
        try {
            Files.deleteIfExists(videoFile);
            Files.deleteIfExists(audioFile);
            log.debug("🗑️ Deleted temporary video and audio files | TaskID: {}", taskId);
        } catch (Exception e) {
            log.warn("⚠️ Could not delete temporary files | TaskID: {}", taskId, e);
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("✅ Video and audio merged successfully | TaskID: {} | Output: {} | Duration: {}ms", 
                taskId, outputFile.getFileName(), duration);
        return outputFile;
    }

    /**
     * Проверить, есть ли ffmpeg в системе.
     */
    public static boolean isFFmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            boolean finished = p.waitFor(5, TimeUnit.SECONDS);
            return finished && p.exitValue() == 0;
        } catch (Exception e) {
            log.warn("ffmpeg not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Получить информацию о медиа файле с помощью ffprobe.
     */
    public static MediaInfo getMediaInfo(Path file) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add("ffprobe");
        cmd.add("-v");
        cmd.add("error");
        cmd.add("-show_entries");
        cmd.add("stream=codec_type,codec_name");
        cmd.add("-of");
        cmd.add("default=noprint_wrappers=1:nokey=1:noprint_wrappers=1");
        cmd.add(file.toString());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        StringBuilder output = new StringBuilder();
        try (var reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean finished = p.waitFor(10, TimeUnit.SECONDS);
        if (!finished || p.exitValue() != 0) {
            return null;
        }

        MediaInfo info = new MediaInfo();
        String[] lines = output.toString().trim().split("\n");
        for (String line : lines) {
            if (line.contains("video")) {
                info.hasVideo = true;
            }
            if (line.contains("audio")) {
                info.hasAudio = true;
            }
        }
        return info;
    }

    /**
     * Информация о медиа файле.
     */
    public static class MediaInfo {
        public boolean hasVideo = false;
        public boolean hasAudio = false;

        @Override
        public String toString() {
            return "MediaInfo{" +
                    "hasVideo=" + hasVideo +
                    ", hasAudio=" + hasAudio +
                    '}';
        }
    }
}
