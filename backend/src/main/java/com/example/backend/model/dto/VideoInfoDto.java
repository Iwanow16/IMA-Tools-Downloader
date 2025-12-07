package com.example.backend.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoInfoDto {
    private String id;
    private String title;
    private String description;
    private String author;
    private String channelId;
    private Integer duration;
    private String thumbnailUrl;
    private Long viewCount;
    private String serviceName;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadDate;
    
    private List<VideoFormatDto> formats;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoFormatDto {
        private String formatId;
        private String extension;
        private String resolution;
        private String qualityLabel;
        private Long fileSize;
        private String formatNote;
        private Double fps;
        private Integer width;
        private Integer height;
        private String vcodec;
        private String acodec;
        private Double tbr;
        private Double abr;
        private Double vbr;
        private String format;
        private String protocol;
        private Boolean isAudioOnly;
    }
}