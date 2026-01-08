package com.iwanow16.backend.model.dto;

public class DownloadRequestDto {
    private String url;
    private String formatId;
    private String quality;
    
    // Time range options
    private boolean timeRangeEnabled;
    private String startTime;
    private String endTime;
    
    // Frame extraction options
    private boolean frameExtractionEnabled;
    private String frameTime;

    public DownloadRequestDto() {}

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public String getFormatId() { return formatId; }
    public void setFormatId(String formatId) { this.formatId = formatId; }
    
    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }
    
    public boolean isTimeRangeEnabled() { return timeRangeEnabled; }
    public void setTimeRangeEnabled(boolean timeRangeEnabled) { this.timeRangeEnabled = timeRangeEnabled; }
    
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    
    public boolean isFrameExtractionEnabled() { return frameExtractionEnabled; }
    public void setFrameExtractionEnabled(boolean frameExtractionEnabled) { this.frameExtractionEnabled = frameExtractionEnabled; }
    
    public String getFrameTime() { return frameTime; }
    public void setFrameTime(String frameTime) { this.frameTime = frameTime; }
}
