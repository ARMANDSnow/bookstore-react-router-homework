package com.homework.bookstore.security;

import com.homework.bookstore.dto.UserResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** 只保存签名密钥，不保存 token、用户会话或登录记录。 */
@Service
public class JwtTokenService {
    private static final String ISSUER = "bookstore";
    private final SecretKey key;
    private final long ttlSeconds;

    public JwtTokenService(@Value("${bookstore.jwt.secret:}") String secret,
                           @Value("${bookstore.jwt.ttl-seconds:3600}") long ttlSeconds) {
        // 【作业4 · 得分点4.3：签名密钥与有效期】
        // TTL是凭证可使用的秒数；范围检查防止配置出无限或负有效期。
        // 无配置时生成本进程随机密钥，适合本机演示；重启会换密钥，旧凭证自然失效。
        // 多实例互认必须使用同一安全配置密钥，并保持发行方和时钟等校验条件一致。
        if (ttlSeconds < 1 || ttlSeconds > 86400) {
            throw new IllegalArgumentException("JWT 有效期必须为 1 到 86400 秒");
        }
        this.key = secret.isBlank() ? Jwts.SIG.HS256.key().build()
            : Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.ttlSeconds = ttlSeconds;
    }

    public String issue(UserResponse user) {
        // 【作业4 · 得分点4.3：从已认证用户生成JWT】
        // 调用方TokenController已通过UserService校验密码，本方法不接受任意客户端身份。
        // issuer标明发行方，subject保存用户名，uid是用户ID，iat/exp记录签发与过期时间。
        // signWith用HS256为头部和载荷签名，compact生成“头部.载荷.签名”字符串；
        // 签名防止内容被修改，载荷本身没有加密。返回客户端后，本服务不保存token列表。
        Instant now = Instant.now();
        return Jwts.builder().issuer(ISSUER).subject(user.getUsername())
            .claim("uid", user.getId()).issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(ttlSeconds)))
            .signWith(key, Jwts.SIG.HS256).compact();
    }

    public String verify(String token) {
        // 【作业4 · 得分点4.4：每次请求重新验签】
        // parser先校验签名、发行方和已有exp的过期状态，再取出可信载荷。
        // 下方额外要求subject非空且必须有exp，防止接受没有身份或永久有效的凭证。
        // 校验失败由JWT库/本方法抛异常，Filter统一转401，不进入图书业务处理。
        // 本实现没有注销黑名单或每次查用户状态；无状态验签不等同于即时撤销所有旧凭证。
        Claims claims = Jwts.parser().verifyWith(key).requireIssuer(ISSUER).build()
            .parseSignedClaims(token).getPayload();
        if (claims.getSubject() == null || claims.getSubject().isBlank() || claims.getExpiration() == null) {
            throw new JwtException("Token 缺少身份或有效期");
        }
        return claims.getSubject();
    }

    public long ttlSeconds() { return ttlSeconds; }
}
