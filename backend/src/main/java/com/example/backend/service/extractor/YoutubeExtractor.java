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
public class YoutubeExtractor implements VideoExtractor {
    
    @Value("${downloader.yt-dlp-path:/usr/local/bin/yt-dlp}")
    private String ytDlpPath;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String[] SUPPORTED_DOMAINS = {
        "youtube.com",
        "youtu.be",
        "m.youtube.com",
        "youtube-nocookie.com"
    };
    
    @Override
    public VideoInfoDto extractInfo(String url) throws ServiceException {
        log.info("Extracting YouTube video info from URL: {}", url);
        
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
            
            VideoInfoDto videoInfo = VideoInfoDto.builder()
                .id(root.path("id").asText())
                .title(root.path("title").asText())
                .description(root.path("description").asText())
                .author(root.path("uploader").asText())
                .channelId(root.path("channel_id").asText())
                .duration(root.path("duration").asInt())
                .thumbnailUrl(root.path("thumbnail").asText())
                .viewCount(root.path("view_count").asLong())
                .serviceName("youtube")
                .build();
            
            // Parse formats
            videoInfo.setFormats(parseFormats(root.path("formats")));
            
            log.info("Successfully extracted info for YouTube video: {}", videoInfo.getTitle());
            return videoInfo;
            
        } catch (Exception e) {
            log.error("Failed to extract YouTube video info: {}", e.getMessage(), e);
            throw new ServiceException("Failed to extract video info: " + e.getMessage(), "YOUTUBE_EXTRACT_ERROR");
        }
    }
    
    @Override
    public boolean supports(String url) {
        return Arrays.stream(SUPPORTED_DOMAINS)
            .anyMatch(domain -> url.contains(domain));
    }
    
    @Override
    public String getServiceName() {
        return "youtube";
    }
    
    @Override
    public String getServiceDisplayName() {
        return "YouTube";
    }
    
    @Override
    public void validateUrl(String url) throws ServiceException {
        if (!supports(url)) {
            throw new ServiceException("Invalid YouTube URL", "INVALID_YOUTUBE_URL");
        }
        
        // Basic YouTube URL validation
        if (!url.contains("/watch?v=") && !url.contains("youtu.be/")) {
            throw new ServiceException("Invalid YouTube video URL format", "INVALID_YOUTUBE_FORMAT");
        }
    }
    
    @Override
    public String[] getSupportedDomains() {
        return SUPPORTED_DOMAINS.clone();
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