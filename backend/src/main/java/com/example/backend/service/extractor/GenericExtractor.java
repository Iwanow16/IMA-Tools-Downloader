package com.example.backend.service.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.backend.model.dto.VideoInfoDto;
import com.example.backend.exception.ServiceException;
import com.example.backend.util.ProcessExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class GenericExtractor implements VideoExtractor {
    
    @Value("${downloader.yt-dlp-path:/usr/local/bin/yt-dlp}")
    private String ytDlpPath;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public VideoInfoDto extractInfo(String url) throws ServiceException {
        log.info("Extracting generic video info from URL: {}", url);
        
        try {
            List<String> command = Arrays.asList(
                ytDlpPath,
                "-j", // JSON output
                "--no-playlist",
                "--ignore-errors",
                url
            );
            
            String jsonOutput = ProcessExecutor.executeCommand(command);
            JsonNode root = objectMapper.readTree(jsonOutput);
            
            // Try to detect service from extractor field
            String serviceName = "generic";
            String extractor = root.path("extractor").asText();
            if (!extractor.isEmpty()) {
                serviceName = extractor.toLowerCase();
            }
            
            VideoInfoDto videoInfo = VideoInfoDto.builder()
                .id(root.path("id").asText())
                .title(root.path("title").asText())
                .description(root.path("description").asText())
                .author(root.path("uploader").asText())
                .duration(root.path("duration").asInt())
                .thumbnailUrl(root.path("thumbnail").asText())
                .viewCount(root.path("view_count").asLong())
                .serviceName(serviceName)
                .build();
            
            // Parse formats
            videoInfo.setFormats(parseFormats(root.path("formats")));
            
            log.info("Successfully extracted info for {} video: {}", serviceName, videoInfo.getTitle());
            return videoInfo;
            
        } catch (Exception e) {
            log.error("Failed to extract generic video info: {}", e.getMessage(), e);
            throw new ServiceException("Failed to extract video info: " + e.getMessage(), "GENERIC_EXTRACT_ERROR");
        }
    }
    
    @Override
    public boolean supports(String url) {
        // Generic extractor supports all URLs as fallback
        return true;
    }
    
    @Override
    public String getServiceName() {
        return "generic";
    }
    
    @Override
    public String getServiceDisplayName() {
        return "Generic Video Service";
    }
    
    @Override
    public void validateUrl(String url) throws ServiceException {
        // Basic URL validation
        if (url == null || url.trim().isEmpty()) {
            throw new ServiceException("URL cannot be empty", "EMPTY_URL");
        }
        
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new ServiceException("Invalid URL format", "INVALID_URL_FORMAT");
        }
    }
    
    @Override
    public String[] getSupportedDomains() {
        return new String[0]; // Generic supports all domains
    }
    
    private List<VideoInfoDto.VideoFormatDto> parseFormats(JsonNode formatsNode) {
        List<VideoInfoDto.VideoFormatDto> formats = new ArrayList<>();
        
        if (formatsNode.isArray()) {
            for (JsonNode format : formatsNode) {
                VideoInfoDto.VideoFormatDto videoFormat = VideoInfoDto.VideoFormatDto.builder()
                    .formatId(format.path("format_id").asText())
                    .extension(format.path("ext").asText())
                    .resolution(format.path("resolution").asText())
                    .qualityLabel(format.path("quality_label").asText())
                    .fileSize(format.path("filesize").asLong())
                    .formatNote(format.path("format_note").asText())
                    .fps(format.path("fps").asDouble())
                    .width(format.path("width").asInt())
                    .height(format.path("height").asInt())
                    .vcodec(format.path("vcodec").asText())
                    .acodec(format.path("acodec").asText())
                    .tbr(format.path("tbr").asDouble())
                    .abr(format.path("abr").asDouble())
                    .vbr(format.path("vbr").asDouble())
                    .format(format.path("format").asText())
                    .protocol(format.path("protocol").asText())
                    .isAudioOnly("audio only".equalsIgnoreCase(format.path("format_note").asText()))
                    .build();
                
                formats.add(videoFormat);
            }
        }
        
        return formats;
    }
}