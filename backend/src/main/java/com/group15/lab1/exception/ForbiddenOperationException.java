package com.group15.lab1.exception;

/**
 * 权限不足异常。
 * 用户尝试执行无权限操作时抛出。
 */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }

    public ForbiddenOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
