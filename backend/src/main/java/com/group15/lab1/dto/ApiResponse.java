package com.group15.lab1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一API响应包装类
 * 所有API返回都通过此包装，包含状态码、消息和数据
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private Integer code;
    private String message;
    private T data;
    private Long timestamp;

    // 构造函数
    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public ApiResponse(Integer code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public ApiResponse(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    // 静态工厂方法：成功响应
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "操作成功", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "操作成功");
    }

    // 静态工厂方法：失败响应
    public static <T> ApiResponse<T> fail(Integer code, String message) {
        return new ApiResponse<>(code, message);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(400, message);
    }

    // 静态工厂方法：常见错误
    public static <T> ApiResponse<T> unauthorized(String message) {
        return new ApiResponse<>(401, message != null ? message : "未授权，请登录");
    }

    public static <T> ApiResponse<T> forbidden(String message) {
        return new ApiResponse<>(403, message != null ? message : "禁止访问");
    }

    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(404, message != null ? message : "资源不存在");
    }

    public static <T> ApiResponse<T> conflict(String message) {
        return new ApiResponse<>(409, message != null ? message : "资源已存在");
    }

    public static <T> ApiResponse<T> serverError(String message) {
        return new ApiResponse<>(500, message != null ? message : "服务器内部错误");
    }

    // Getter & Setter
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ApiResponse{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", timestamp=" + timestamp +
                '}';
    }
}
