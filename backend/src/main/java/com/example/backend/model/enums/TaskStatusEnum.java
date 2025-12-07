package com.example.backend.model.enums;

public enum TaskStatusEnum {
    PENDING("Pending"),
    PROCESSING("Processing"),
    DOWNLOADING("Downloading"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    CANCELLED("Cancelled"),
    QUEUED("Queued");
    
    private final String displayName;
    
    TaskStatusEnum(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static boolean isFinalStatus(String status) {
        return status.equals(COMPLETED.name()) ||
               status.equals(FAILED.name()) ||
               status.equals(CANCELLED.name());
    }
}