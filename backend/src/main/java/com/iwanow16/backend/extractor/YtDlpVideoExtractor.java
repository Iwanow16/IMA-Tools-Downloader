package com.iwanow16.backend.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iwanow16.backend.model.dto.VideoInfoDto;
import com.iwanow16.backend.model.dto.FormatDto;
import com.iwanow16.backend.util.ProcessExecutor;
import com.iwanow16.backend.util.FormatEnhancer;
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

    @Value("${youtube.use-remote-components:true}")
    private boolean useRemoteComponents;

    @Value("${youtube.merge-audio:false}")
    private boolean mergeAudio;

    @Override
    public VideoInfoDto extractInfo(String url) throws Exception {
        log.info("🎥 YtDlp: Extracting video info from: {}", url);
        
        List<String> cmd = new ArrayList<>();
        cmd.add("yt-dlp");

        // Не скачивать весь плейлист, даже если URL содержит параметры плейлиста
        cmd.add("--no-playlist");

        // Добавить JS runtime для YouTube (требуется для новых версий)
        if (jsRuntime != null && !jsRuntime.isBlank()) {
            cmd.add("--js-runtimes");
            cmd.add(jsRuntime);
            log.debug("Using JS runtime: {}", jsRuntime);
        }

        // Добавить remote components для решения JS challenges
        if (useRemoteComponents) {
            cmd.add("--remote-components");
            cmd.add("ejs:github");
            log.debug("📡 Remote EJS components enabled");
        }

        // Добавить cookies, если файл существует и валиден (> 100 байт)
        if (cookiesFile != null && !cookiesFile.isBlank()) {
            java.io.File cookieFileObj = new java.io.File(cookiesFile);
            if (cookieFileObj.exists() && cookieFileObj.length() > 100) {
                cmd.add("--cookies");
                cmd.add(cookiesFile);
            } else {
                log.debug("Cookies file not found or empty, proceeding without cookies");
            }
        }

        cmd.add("--dump-json");
        cmd.add(url);

        StringBuilder out = new StringBuilder();
        long startTime = System.currentTimeMillis();
        log.debug("Running yt-dlp command...");
        
        int rc = ProcessExecutor.run(cmd, 30, out);
        long duration = System.currentTimeMillis() - startTime;
        
        if (rc != 0) {
            String errorOutput = out.toString();
            log.error("yt-dlp failed with code {}: {} (Duration: {}ms)", rc, errorOutput, duration);
            throw new RuntimeException("yt-dlp failed with code " + rc + ": " + errorOutput);
        }

        String json = out.toString().trim();
        // yt-dlp can output warnings before JSON.
        // Find first line that looks like JSON (starts with '{' or '[').
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
            log.error("Could not find JSON output from yt-dlp. Output: {}", json);
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

        // Извлечение форматов - фильтруем и группируем по качеству
        List<FormatDto> formats = new ArrayList<>();
        Map<String, FormatDto> qualityMap = new LinkedHashMap<>();
        
        // Сначала пробуем получить форматы из requested_formats (предварительно выбранные лучшие комбинации)
        JsonNode requestedFormatsNode = node.path("requested_formats");
        JsonNode formatsNode = node.path("formats");
        
        // Используем requested_formats, если они доступны (это лучший выбор от yt-dlp)
        // Иначе используем полный список formats
        JsonNode sourceFormats = (requestedFormatsNode.isArray() && requestedFormatsNode.size() > 0) ? 
                                  requestedFormatsNode : formatsNode;
        
        if (sourceFormats.isArray()) {
            boolean isRequestedFormats = sourceFormats == requestedFormatsNode;
            log.debug("Processing {} formats from {} (mergeAudio={})", 
                    sourceFormats.size(), 
                    isRequestedFormats ? "requested_formats" : "formats",
                    mergeAudio);
            
            for (JsonNode f : sourceFormats) {
                String vcodec = f.path("vcodec").asText("none");
                String acodec = f.path("acodec").asText("none");
                int height = f.path("height").asInt(0);
                
                // Skip formats without video or without resolution
                if ("none".equals(vcodec) || height == 0) {
                    continue;
                }
                
                // Create FormatDto
                FormatDto format = new FormatDto();
                format.setFormatId(f.path("format_id").asText());
                format.setExt(f.path("ext").asText());
                format.setAcodec(acodec);
                format.setVcodec(vcodec);
                
                long size = f.path("filesize").asLong(0);
                if (size == 0) {
                    size = f.path("filesize_approx").asLong(0);
                }
                format.setFilesize(size);
                
                int fps = f.path("fps").asInt(0);
                int width = f.path("width").asInt(0);
                
                // Format quality string
                String quality = height + "p";
                if (fps > 0) {
                    quality += " (" + fps + "fps)";
                }
                
                // Add audio info
                if (!"none".equals(acodec)) {
                    quality += " + Audio";
                }
                
                format.setQuality(quality);
                format.setResolution(width + "x" + height);
                
                // Group by resolution + audio, choose best
                String qualityKey = height + "p_" + (!"none".equals(acodec) ? "audio" : "noaudio");
                
                // Replace only if first format or new has better codec
                if (!qualityMap.containsKey(qualityKey)) {
                    qualityMap.put(qualityKey, format);
                } else {
                    // Compare by bitrate (if new is bigger, update)
                    if (size > qualityMap.get(qualityKey).getFilesize()) {
                        qualityMap.put(qualityKey, format);
                    }
                }
            }
        }
        
        // Convert to list
        formats = new ArrayList<>(qualityMap.values());
        
        // Enhance formats with synthetic video+audio variants (if needed)
        formats = FormatEnhancer.enhanceFormats(formats, "youtube", mergeAudio);
        
        // Sort by quality (best to worst)
        formats = formats.stream()
                .sorted((a, b) -> {
                    String qualityA = a.getQuality().split("p")[0];
                    String qualityB = b.getQuality().split("p")[0];
                    try {
                        return Integer.compare(Integer.parseInt(qualityB), Integer.parseInt(qualityA));
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .collect(Collectors.toList());
        
        info.setFormats(formats);
        log.info("Successfully extracted video info | Title: {} | Duration: {}s | Formats: {} | Duration: {}ms", 
                info.getTitle(), info.getDurationSeconds(), formats.size(), duration);
        return info;
    }

    @Override
    public String getServiceName() {
        return "youtube";
    }

    @Override
    public boolean supports(String url) {
        return url != null && (url.contains("youtube.com") || url.contains("youtu.be"));
    }
}
