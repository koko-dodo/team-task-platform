package com.group15.lab1.exception;

/**
 * 冲突异常。
 * 用户已存在、重复操作等冲突场景。
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
