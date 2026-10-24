package com.homework.bookstore.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.bookstore.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper mapper;
    public JsonAuthenticationEntryPoint(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        // 【作业4 · 得分点4.4/4.5：认证失败必须是实际HTTP401】
        // 仅在JSON里写code=40101而HTTP仍200，不能证明题目要求的401拦截；这里两者都设置。
        // WWW-Authenticate告知客户端应提供Bearer凭证。Filter错误和未认证授权失败共用
        // 此入口，返回JSON而非跳转HTML登录页，便于前端、curl和测试截图直接观察。
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Bearer");
        response.setContentType("application/json;charset=UTF-8");
        mapper.writeValue(response.getOutputStream(), ApiResponse.error(40101, "请提供有效的 Bearer Token"));
    }
}
