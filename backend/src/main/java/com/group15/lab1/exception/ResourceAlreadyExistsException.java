package com.group15.lab1.exception;

/**
 * 资源已存在异常
 */
public class ResourceAlreadyExistsException extends RuntimeException {

    public ResourceAlreadyExistsException(String message) {
        super(message);
    }
}
