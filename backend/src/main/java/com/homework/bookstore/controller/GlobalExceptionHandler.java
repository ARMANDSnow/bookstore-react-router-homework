package com.homework.bookstore.controller;

import com.homework.bookstore.dto.ApiResponse;
import com.homework.bookstore.service.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <h2>全局异常 → ApiResponse 翻译层</h2>
 *
 * <p>{@code @RestControllerAdvice} 是 {@code @ControllerAdvice} + {@code @ResponseBody} 的组合：
 * 拦截所有 {@code @RestController} 抛出的异常，统一翻译为 ApiResponse JSON，
 * 让 Controller 业务代码可以「自由抛异常」而不写一堆 try/catch。
 *
 * <h3>捕获优先级（Spring 自上而下匹配最近的父类）</h3>
 * <ol>
 *   <li>{@link com.homework.bookstore.service.exception.BusinessException}
 *       → 翻译为对应 HTTP 状态码 + 业务错误码</li>
 *   <li>{@link org.springframework.web.bind.MethodArgumentNotValidException}
 *       → JSR-380 校验失败（如 {@code @NotBlank} 未通过），HTTP 400 + 错误码 40000</li>
 *   <li>{@code Exception}（兜底）
 *       → 500 + 错误码 50000，避免堆栈直接暴露给前端</li>
 * </ol>
 *
 * <h3>怎么测？</h3>
 * <pre>
 *   curl -X POST localhost:8080/api/v1/users/login -H 'Content-Type: application/json' -d '{"username":"x","password":"y"}'
 *   # → 401 {"code":40101,"message":"用户名或密码错误","data":null}
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        int family = ex.getCode() / 100;
        HttpStatus status = family == 401
                ? HttpStatus.UNAUTHORIZED
                : (family == 403
                ? HttpStatus.FORBIDDEN
                : (family == 404 ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST));
        return ResponseEntity.status(status).body(ApiResponse.error(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getDefaultMessage())
                .orElse("参数校验失败");
        return ResponseEntity.badRequest().body(ApiResponse.error(40000, msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleOther(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(50000, "服务器内部错误：" + ex.getMessage()));
    }
}
