package com.swift.common;

import lombok.Data;

/**
 * 统一API返回结果封装
 */
@Data
public class Result<T> {
    
    private int code;
    private String message;
    private T data;
    
    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    // 成功返回
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }
    
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }
    
    // 失败返回
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
    
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }
    
    // 业务错误返回
    public static <T> Result<T> businessError(String message) {
        return new Result<>(400, message, null);
    }
    
    public static <T> Result<T> notFound(String message) {
        return new Result<>(404, message, null);
    }
    
    public static <T> Result<T> conflict(String message) {
        return new Result<>(409, message, null);
    }
}