package com.iwanow16.backend.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FormatDto {
    @JsonProperty("format_id")
    private String formatId;

    private String ext;

    @JsonProperty("note")
    private String note;

    private String resolution;
    private String acodec;
    private String vcodec;
    private long filesize;

    private String quality;

    public String getFormatId() { return formatId; }
    public void setFormatId(String formatId) { this.formatId = formatId; }
    public String getExt() { return ext; }
    public void setExt(String ext) { this.ext = ext; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    public String getAcodec() { return acodec; }
    public void setAcodec(String acodec) { this.acodec = acodec; }
    public String getVcodec() { return vcodec; }
    public void setVcodec(String vcodec) { this.vcodec = vcodec; }
    public long getFilesize() { return filesize; }
    public void setFilesize(long filesize) { this.filesize = filesize; }
    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }
}
