package com.group15.lab1.exception;

/**
 * 无效凭据异常
 * 用户名或密码错误时抛出
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}