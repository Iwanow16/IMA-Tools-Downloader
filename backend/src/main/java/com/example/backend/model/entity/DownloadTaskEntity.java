package com.example.backend.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "download_tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadTaskEntity {
    
    @Id
    @Column(name = "task_id")
    private String taskId;
    
    @Column(name = "url", length = 2048)
    private String url;
    
    @Column(name = "title")
    private String title;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "progress")
    private Integer progress;
    
    @Column(name = "service_name")
    private String serviceName;
    
    @Column(name = "format_id")
    private String formatId;
    
    @Column(name = "quality")
    private String quality;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "file_path")
    private String filePath;
    
    @Column(name = "filename")
    private String filename;
    
    @Column(name = "error_message", length = 4000)
    private String errorMessage;
    
    @Column(name = "client_ip")
    private String clientIp;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "download_url")
    private String downloadUrl;
    
    @Column(name = "thumbnail_url")
    private String thumbnailUrl;
    
    @Column(name = "duration")
    private String duration;
    
    @Column(name = "is_audio_only")
    private Boolean audioOnly;
    
    @PrePersist
    protected void onCreate() {
        if (taskId == null) {
            taskId = java.util.UUID.randomUUID().toString();
        }
    }
}