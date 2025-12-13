package com.iwanow16.backend.model.dto;


import java.util.List;

public class VideoInfoDto {
    private String id;
    private String title;
    private long durationSeconds;
    private long filesize;
    private String thumbnail;
    private List<FormatDto> formats;

    public VideoInfoDto() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(long durationSeconds) { this.durationSeconds = durationSeconds; }
    public long getFilesize() { return filesize; }
    public void setFilesize(long filesize) { this.filesize = filesize; }
    public String getThumbnail() { return thumbnail; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }
    public List<FormatDto> getFormats() { return formats; }
    public void setFormats(List<FormatDto> formats) { this.formats = formats; }
}
