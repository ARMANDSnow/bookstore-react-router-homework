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
        if (ttlSeconds < 1 || ttlSeconds > 86400) {
            throw new IllegalArgumentException("JWT 有效期必须为 1 到 86400 秒");
        }
        this.key = secret.isBlank() ? Jwts.SIG.HS256.key().build()
            : Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.ttlSeconds = ttlSeconds;
    }

    public String issue(UserResponse user) {
        Instant now = Instant.now();
        return Jwts.builder().issuer(ISSUER).subject(user.getUsername())
            .claim("uid", user.getId()).issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(ttlSeconds)))
            .signWith(key, Jwts.SIG.HS256).compact();
    }

    public String verify(String token) {
        Claims claims = Jwts.parser().verifyWith(key).requireIssuer(ISSUER).build()
            .parseSignedClaims(token).getPayload();
        if (claims.getSubject() == null || claims.getSubject().isBlank() || claims.getExpiration() == null) {
            throw new JwtException("Token 缺少身份或有效期");
        }
        return claims.getSubject();
    }

    public long ttlSeconds() { return ttlSeconds; }
}
