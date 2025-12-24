package com.iwanow16.backend.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iwanow16.backend.model.dto.VideoInfoDto;
import com.iwanow16.backend.model.dto.FormatDto;
import com.iwanow16.backend.util.ProcessExecutor;
import com.iwanow16.backend.util.FormatEnhancer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Экстрактор информации о видео с Bilibili с использованием yt-dlp.
 */
@Component
public class BilibiliVideoExtractor implements VideoExtractor {
    private static final Logger log = LoggerFactory.getLogger(BilibiliVideoExtractor.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public VideoInfoDto extractInfo(String url) throws Exception {
        log.info("🎬 Bilibili: Extracting video info from: {}", url);
        
        String cookiesPath = "/app/resources/bilibili_cookies.txt";
        
        List<String> cmd = new ArrayList<>();
        cmd.add("yt-dlp");
        cmd.add("--dump-json");
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
        cmd.add(url);

        StringBuilder out = new StringBuilder();
        long startTime = System.currentTimeMillis();
        log.debug("⏳ Running yt-dlp command for Bilibili...");
        
        int rc = ProcessExecutor.run(cmd, 30, out);
        long duration = System.currentTimeMillis() - startTime;
        
        if (rc != 0) {
            String errorOutput = out.toString();
            log.error("❌ yt-dlp failed with code {}: {} (Duration: {}ms)", rc, errorOutput, duration);
            throw new RuntimeException("yt-dlp failed with code " + rc + ": " + errorOutput);
        }
        
        String jsonOutput = out.toString();
        log.debug("📊 yt-dlp output length: {} bytes | Duration: {}ms", jsonOutput.length(), duration);
        
        try {
            JsonNode rootNode = mapper.readTree(jsonOutput);
            
            VideoInfoDto info = new VideoInfoDto();
            info.setTitle(rootNode.path("title").asText());
            info.setAuthor(rootNode.path("uploader").asText());
            info.setUrl(url);
            info.setDurationSeconds(rootNode.path("duration").asLong(0));
            
            // Карта для Bilibili format_id → качество
            Map<String, String> bilibiliQualityMap = new LinkedHashMap<>();
            bilibiliQualityMap.put("30216", "Audio - 66kbps");
            bilibiliQualityMap.put("30232", "Audio - 132kbps");
            bilibiliQualityMap.put("30280", "Audio - 132kbps");
            bilibiliQualityMap.put("30011", "360P - H.265");
            bilibiliQualityMap.put("30016", "360P - H.264");
            bilibiliQualityMap.put("30033", "480P - H.265");
            bilibiliQualityMap.put("30032", "480P - H.264");
            bilibiliQualityMap.put("30066", "720P - H.265");
            bilibiliQualityMap.put("30064", "720P - H.264");
            bilibiliQualityMap.put("30077", "1080P - H.265");
            bilibiliQualityMap.put("30080", "1080P - H.264");
            
            // Извлечь форматы
            JsonNode formatsNode = rootNode.path("formats");
            List<FormatDto> formats = new ArrayList<>();
            
            if (formatsNode.isArray()) {
                for (JsonNode formatNode : formatsNode) {
                    FormatDto format = new FormatDto();
                    String formatId = formatNode.path("format_id").asText();
                    format.setFormatId(formatId);
                    
                    // Получить качество из карты или из полей
                    String quality = bilibiliQualityMap.getOrDefault(formatId, 
                        formatNode.path("format_note").asText(""));
                    
                    // Если quality все еще пусто, построить из разрешения
                    if (quality.isEmpty()) {
                        int width = formatNode.path("width").asInt(0);
                        int height = formatNode.path("height").asInt(0);
                        if (width > 0 && height > 0) {
                            quality = height + "P";
                        } else {
                            quality = formatNode.path("format").asText("Unknown");
                        }
                    }
                    
                    format.setQuality(quality);
                    format.setExt(formatNode.path("ext").asText());
                    
                    // Получить информацию о кодеках
                    String vcodec = formatNode.path("vcodec").asText();
                    String acodec = formatNode.path("acodec").asText();
                    
                    // Установить vcodec и acodec в формат (для фронтенда)
                    format.setVcodec(vcodec);
                    format.setAcodec(acodec);
                    
                    // Установить note с информацией о кодеках
                    if (!vcodec.isEmpty() || !acodec.isEmpty()) {
                        format.setNote((vcodec.isEmpty() ? "" : "V: " + vcodec) + 
                                      (acodec.isEmpty() ? "" : (vcodec.isEmpty() ? "" : " ") + "A: " + acodec));
                    }
                    
                    long filesize = formatNode.path("filesize").asLong(0);
                    if (filesize == 0) {
                        filesize = formatNode.path("filesize_approx").asLong(0);
                    }
                    format.setFilesize(filesize);
                    
                    formats.add(format);
                }
            }
            
            // Удалить дубликаты и оставить лучшие форматы
            formats = formats.stream()
                    .filter(f -> f.getFormatId() != null && !f.getFormatId().isEmpty())
                    .collect(Collectors.toList());
            
            // Обогатить форматы синтетическими video+audio вариантами
            formats = FormatEnhancer.enhanceFormats(formats, "bilibili");
            
            info.setFormats(formats);
            log.info("✅ Successfully extracted video info | Title: {} | Duration: {}s | Formats: {} | Duration: {}ms", 
                    info.getTitle(), info.getDurationSeconds(), formats.size(), duration);
            return info;
        } catch (Exception e) {
            log.error("❌ Failed to parse yt-dlp output: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse video info: " + e.getMessage(), e);
        }
    }

    @Override
    public String getServiceName() {
        return "bilibili";
    }

    @Override
    public boolean supports(String url) {
        return url != null && (url.contains("bilibili.com") || url.contains("b23.tv"));
    }
}
