package com.iwanow16.backend.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Утилита для извлечения кадров из видео в формате PNG с использованием ffmpeg.
 */
@Component
public class FrameExtractorUtil {
    private static final Logger log = LoggerFactory.getLogger(FrameExtractorUtil.class);

    /**
     * Извлечь кадр из видео по указанному времени.
     * @param videoPath Путь к видео файлу
     * @param frameTime Время кадра в секундах
     * @param outputDir Директория для сохранения кадра
     * @param taskId ID задачи для логирования
     * @return Путь к извлеченному кадру
     * @throws Exception Если извлечение не удалось
     */
    public Path extractFrame(String videoPath, String frameTime, Path outputDir, String taskId) throws Exception {
        log.info("🎬 Extracting frame | TaskID: {} | Time: {}s | Video: {}", taskId, frameTime, videoPath);
        
        try {
            // Парсим время
            double time;
            try {
                time = Double.parseDouble(frameTime);
            } catch (NumberFormatException e) {
                log.error("❌ Invalid frame time format | TaskID: {} | Time: {}", taskId, frameTime);
                throw new IllegalArgumentException("Invalid time format: " + frameTime);
            }

            if (time < 0) {
                throw new IllegalArgumentException("Frame time cannot be negative");
            }

            // Генерируем имя выходного файла
            String outputFilename = "frame_" + UUID.randomUUID() + ".png";
            Path outputPath = outputDir.resolve(outputFilename);

            // Форматируем время для ffmpeg (HH:MM:SS.ms)
            String timeStr = formatTimeForFFmpeg(time);

            // Построить команду ffmpeg
            List<String> cmd = new ArrayList<>();
            cmd.add("ffmpeg");
            cmd.add("-ss");
            cmd.add(timeStr);
            cmd.add("-i");
            cmd.add(videoPath);
            cmd.add("-vframes");
            cmd.add("1");
            cmd.add("-q:v");
            cmd.add("2");  // Высокое качество
            cmd.add("-f");
            cmd.add("image2");
            cmd.add(outputPath.toString());

            log.debug("⏳ Executing ffmpeg command | TaskID: {} | Output: {}", taskId, outputFilename);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            // Читать вывод
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.debug("📊 ffmpeg output | TaskID: {} | {}", taskId, line);
                }
            }

            int exitCode = p.waitFor();
            
            if (exitCode != 0) {
                log.error("❌ FFmpeg failed | TaskID: {} | Exit code: {} | Output: {}", 
                        taskId, exitCode, output);
                throw new RuntimeException("FFmpeg failed with exit code: " + exitCode);
            }

            // Проверить, что файл был создан
            if (!Files.exists(outputPath)) {
                log.error("❌ Frame file not created | TaskID: {} | Expected path: {}", taskId, outputPath);
                throw new RuntimeException("Frame extraction failed: output file not created");
            }

            long fileSize = Files.size(outputPath);
            log.info("✅ Frame extracted successfully | TaskID: {} | File: {} | Size: {} bytes", 
                    taskId, outputFilename, fileSize);
            
            return outputPath;

        } catch (Exception e) {
            log.error("❌ Frame extraction failed | TaskID: {} | Error: {}", taskId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Форматировать время в секундах в формат HH:MM:SS.mm для ffmpeg.
     */
    private String formatTimeForFFmpeg(double seconds) {
        long totalSeconds = (long) seconds;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long secs = totalSeconds % 60;
        long milliseconds = (long) ((seconds - totalSeconds) * 1000);

        return String.format("%02d:%02d:%02d.%03d", hours, minutes, secs, milliseconds);
    }
}
