package com.iwanow16.backend.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class VideoInfoDto {
    private String id;
    private String url;
    private String title;
    private String author;

    @JsonProperty("duration")
    private long durationSeconds;

    private long filesize;
    private String thumbnail;
    private List<FormatDto> formats;

    public VideoInfoDto() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(long durationSeconds) { this.durationSeconds = durationSeconds; }
    public long getFilesize() { return filesize; }
    public void setFilesize(long filesize) { this.filesize = filesize; }
    public String getThumbnail() { return thumbnail; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }
    public List<FormatDto> getFormats() { return formats; }
    public void setFormats(List<FormatDto> formats) { this.formats = formats; }
}
