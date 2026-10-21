package com.homework.bookstore.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** 每次重新验签，SecurityContext 只在本次请求内有效，不写 HttpSession。 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenService tokens;
    private final JsonAuthenticationEntryPoint unauthorized;
    public JwtAuthenticationFilter(JwtTokenService tokens, JsonAuthenticationEntryPoint unauthorized) {
        this.tokens = tokens;
        this.unauthorized = unauthorized;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return "OPTIONS".equals(request.getMethod())
            || !(path.equals("/api/secure") || path.startsWith("/api/secure/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null) {
            try {
                if (!header.regionMatches(true, 0, "Bearer ", 0, 7) || header.length() > 4096) {
                    throw new BadCredentialsException("Authorization 格式错误");
                }
                String username = tokens.verify(header.substring(7));
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(new UsernamePasswordAuthenticationToken(username, null, List.of()));
                SecurityContextHolder.setContext(context);
            } catch (JwtException | IllegalArgumentException | BadCredentialsException exception) {
                SecurityContextHolder.clearContext();
                unauthorized.commence(request, response, new BadCredentialsException("Token 无效"));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
