package com.homework.bookstore.service.exception;

/**
 * 通用业务异常：被 GlobalExceptionHandler 翻译为 ApiResponse 错误响应。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
