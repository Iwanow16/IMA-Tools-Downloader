package com.iwanow16.backend.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iwanow16.backend.model.dto.VideoInfoDto;
import com.iwanow16.backend.model.dto.FormatDto;
import com.iwanow16.backend.util.ProcessExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

/**
 * Экстрактор информации о видео с YouTube с использованием yt-dlp.
 * Поддерживает извлечение метаданных и списка доступных форматов.
 */
@Component
public class YtDlpVideoExtractor implements VideoExtractor {
    private static final Logger log = LoggerFactory.getLogger(YtDlpVideoExtractor.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${youtube.cookies-file:}")
    private String cookiesFile;

    @Value("${youtube.js-runtime:node}")
    private String jsRuntime;

    @Override
    public VideoInfoDto extractInfo(String url) throws Exception {
        log.info("🎥 YtDlp: Extracting video info from: {}", url);
        
        List<String> cmd = new ArrayList<>();
        cmd.add("yt-dlp");

        // Добавить cookies, если они настроены
        if (cookiesFile != null && !cookiesFile.isBlank()) {
            cmd.add("--cookies");
            cmd.add(cookiesFile);
            log.debug("🍪 Using cookies file: {}", cookiesFile);
        }

        cmd.add("--dump-json");
        cmd.add(url);

        StringBuilder out = new StringBuilder();
        long startTime = System.currentTimeMillis();
        log.debug("⏳ Running yt-dlp command...");
        
        int rc = ProcessExecutor.run(cmd, 30, out);
        long duration = System.currentTimeMillis() - startTime;
        
        if (rc != 0) {
            String errorOutput = out.toString();
            log.error("❌ yt-dlp failed with code {}: {} (Duration: {}ms)", rc, errorOutput, duration);
            throw new RuntimeException("yt-dlp failed with code " + rc + ": " + errorOutput);
        }

        String json = out.toString().trim();
        // yt-dlp может выводить предупреждения перед JSON.
        // Найти первую строку, которая выглядит как JSON (начинается с '{' или '[').
        String[] lines = json.split("\n");
        String jsonLine = null;
        for (String l : lines) {
            String t = l.trim();
            if (t.startsWith("{") || t.startsWith("[")) {
                jsonLine = t;
                break;
            }
        }
        if (jsonLine == null) {
            log.error("❌ Could not find JSON output from yt-dlp. Output: {}", json);
            throw new RuntimeException("Could not find JSON output from yt-dlp. Output: " + json);
        }
        JsonNode node = mapper.readTree(jsonLine);

        VideoInfoDto info = new VideoInfoDto();
        info.setId(node.path("id").asText());
        info.setUrl(url);
        info.setTitle(node.path("title").asText());
        info.setAuthor(node.path("uploader").asText(null));
        info.setDurationSeconds(node.path("duration").asLong(0));
        info.setFilesize(node.path("filesize").asLong(0));
        info.setThumbnail(node.path("thumbnail").asText(null));

        // Извлечение форматов - фильтруем и группируем по качеству (по примеру Python кода)
        List<FormatDto> formats = new ArrayList<>();
        Map<String, FormatDto> qualityMap = new LinkedHashMap<>();
        
        JsonNode formatsNode = node.path("formats");
        if (formatsNode.isArray()) {
            log.debug("📊 Processing {} formats...", formatsNode.size());
            
            for (JsonNode f : formatsNode) {
                // Пропускаем форматы без видео (только полученные форматы с видео кодеком)
                String vcodec = f.path("vcodec").asText("none");
                if ("none".equals(vcodec)) {
                    continue; // Пропускаем форматы без видео
                }
                
                // Пропускаем форматы без разрешения
                int height = f.path("height").asInt(0);
                if (height == 0) {
                    continue;
                }
                
                FormatDto format = new FormatDto();
                format.setFormatId(f.path("format_id").asText());
                format.setExt(f.path("ext").asText());
                
                String acodec = f.path("acodec").asText("none");
                format.setAcodec(acodec);
                format.setVcodec(vcodec);
                
                long size = f.path("filesize").asLong(0);
                if (size == 0) {
                    size = f.path("filesize_approx").asLong(0);
                }
                format.setFilesize(size);
                
                int fps = f.path("fps").asInt(0);
                int width = f.path("width").asInt(0);
                
                // Формируем качество: высота + fps если есть
                String quality = height + "p";
                if (fps > 0) {
                    quality += " (" + fps + "fps)";
                }
                format.setQuality(quality);
                format.setResolution(width + "x" + height);
                
                // Группируем по разрешению, выбираем лучший для каждого качества
                String qualityKey = height + "p";
                if (!qualityMap.containsKey(qualityKey) || 
                    ("none".equals(qualityMap.get(qualityKey).getAcodec()) && !"none".equals(acodec))) {
                    // Заменяем если это первый формат для этого качества
                    // или новый формат имеет аудио, а текущий нет
                    qualityMap.put(qualityKey, format);
                }
            }
        }
        
        // Сортируем по качеству (по убыванию)
        formats = qualityMap.values().stream()
                .sorted((a, b) -> {
                    int heightA = Integer.parseInt(a.getQuality().split("p")[0]);
                    int heightB = Integer.parseInt(b.getQuality().split("p")[0]);
                    return Integer.compare(heightB, heightA);
                })
                .collect(Collectors.toList());
        
        info.setFormats(formats);
        log.info("✅ Successfully extracted video info | Title: {} | Duration: {}s | Formats: {} | Duration: {}ms", 
                info.getTitle(), info.getDurationSeconds(), formats.size(), duration);
        return info;
    }

    @Override
    public String getServiceName() {
        return "youtube";
    }
}
