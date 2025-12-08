package com.iwanow16.backend.model.dto;

public class TaskStatusDto {
    private String taskId;
    private String url;
    private String title;
    private String status;
    private int progress;
    private String filename;

    public TaskStatusDto() {}

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
}
