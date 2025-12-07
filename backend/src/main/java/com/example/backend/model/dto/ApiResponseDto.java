package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDto<T> {
    private boolean success;
    private String message;
    private T data;
    private String errorCode;
    private Long timestamp;
    private Integer statusCode;
    
    public static <T> ApiResponseDto<T> success(T data) {
        return ApiResponseDto.<T>builder()
                .success(true)
                .message("Success")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .statusCode(200)
                .build();
    }
    
    public static <T> ApiResponseDto<T> success(String message, T data) {
        return ApiResponseDto.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .statusCode(200)
                .build();
    }
    
    public static <T> ApiResponseDto<T> error(String message, String errorCode, Integer statusCode) {
        return ApiResponseDto.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .timestamp(System.currentTimeMillis())
                .statusCode(statusCode)
                .build();
    }
}