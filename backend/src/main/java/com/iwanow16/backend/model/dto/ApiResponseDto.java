package com.iwanow16.backend.model.dto;

public class ApiResponseDto<T> {
    private boolean success;
    private String message;
    private T data;
    private Integer statusCode;

    public ApiResponseDto() {}

    public ApiResponseDto(boolean success, String message, T data, Integer statusCode) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.statusCode = statusCode;
    }

    public static <T> ApiResponseDto<T> success(T data) {
        return new ApiResponseDto<>(true, "Success", data, 200);
    }

    public static <T> ApiResponseDto<T> success(String message, T data) {
        return new ApiResponseDto<>(true, message, data, 200);
    }

    public static <T> ApiResponseDto<T> error(String message, Integer statusCode) {
        return new ApiResponseDto<>(false, message, null, statusCode);
    }

    // getters/setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }
}
