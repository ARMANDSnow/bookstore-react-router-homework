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
        // 【作业4 · 得分点4.4：鉴权范围】
        // 仅处理/api/secure及其子路径；OPTIONS是浏览器跨域预检，不要求携带登录凭证。
        // 全仓库的历史公开接口保持兼容，不能因此声称全站都受JWT保护；作业4精简包
        // 只保留登录和受保护图书入口。实际访问权限还由SecurityConfig的规则共同决定。
        String path = request.getServletPath();
        return "OPTIONS".equals(request.getMethod())
            || !(path.equals("/api/secure") || path.startsWith("/api/secure/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // 【作业4 · 得分点4.4：Authorization → 验签 → 本次请求身份】
        // 客户端发送Authorization: Bearer <JWT>。先检查头部形式，再让JWT库校验凭证。
        // 校验成功才创建SecurityContext（安全上下文），供后续权限检查识别当前用户。
        // 这里不是把身份保存为HttpSession；SecurityConfig使用NullSecurityContextRepository，
        // 不持久化此上下文，请求结束由Spring Security清理，下一次仍须重新携带JWT。
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
                // 有头但签名/有效期/格式错误：清掉身份，立即输出401并return，禁止继续查询。
                SecurityContextHolder.clearContext();
                unauthorized.commence(request, response, new BadCredentialsException("Token 无效"));
                return;
            }
        }
        // 没有头时本过滤器不伪造身份：继续到Security的authenticated规则，由它触发401。
        // 所以“没带token”和“带了坏token”最终相同，但分别在授权检查和验签分支被拦截。
        chain.doFilter(request, response);
    }
}
