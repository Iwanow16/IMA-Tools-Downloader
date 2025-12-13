package com.iwanow16.backend.model.dto;

public class DownloadRequestDto {
    private String url;
    private String formatId;
    private String quality;

    public DownloadRequestDto() {}

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getFormatId() { return formatId; }
    public void setFormatId(String formatId) { this.formatId = formatId; }
    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }
}
