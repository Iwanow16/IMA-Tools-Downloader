package com.iwanow16.backend.model.dto;

public class FormatDto {
    private String formatId;
    private String ext;
    private String formatNote;
    private String resolution;
    private String acodec;
    private String vcodec;
    private long filesize;

    public String getFormatId() { return formatId; }
    public void setFormatId(String formatId) { this.formatId = formatId; }
    public String getExt() { return ext; }
    public void setExt(String ext) { this.ext = ext; }
    public String getFormatNote() { return formatNote; }
    public void setFormatNote(String formatNote) { this.formatNote = formatNote; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    public String getAcodec() { return acodec; }
    public void setAcodec(String acodec) { this.acodec = acodec; }
    public String getVcodec() { return vcodec; }
    public void setVcodec(String vcodec) { this.vcodec = vcodec; }
    public long getFilesize() { return filesize; }
    public void setFilesize(long filesize) { this.filesize = filesize; }
}
