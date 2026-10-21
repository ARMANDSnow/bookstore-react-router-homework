package com.homework.bookstore.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.bookstore.dto.BookRequest;
import com.homework.bookstore.dto.RegisterRequest;
import com.homework.bookstore.service.BookService;
import com.homework.bookstore.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.junit.jupiter.api.Assertions.*;

/** 真实 HTTP/Tomcat 测试；MockMvc 的调用线程不能证明 Web 请求使用虚拟线程。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:jwt-http;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
    "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never",
    "spring.threads.virtual.enabled=true", "bookstore.catalog.simulated-delay-ms=100"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StatelessHttpTest {
    private static final SecretKey KEY = Jwts.SIG.HS256.key().build();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("bookstore.jwt.secret", () -> Encoders.BASE64.encode(KEY.getEncoded()));
    }
    @LocalServerPort int port;
    @Autowired BookService books;
    @Autowired UserService users;
    @Autowired ObjectMapper mapper;
    final CookieManager cookies = new CookieManager();
    final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies)
        .connectTimeout(Duration.ofSeconds(5)).build();
    String token;

    @BeforeAll void seedAndLogin() throws Exception {
        var book = new BookRequest();
        book.setId("jwt-book"); book.setTitle("虚拟线程与无状态服务"); book.setAuthor("测试作者");
        book.setStock(8); book.setPrice(new BigDecimal("32.50")); book.setImage("/test.jpg");
        books.createBook(book);
        var user = new RegisterRequest();
        user.setUsername("jwt-reader"); user.setPassword("test-password"); user.setEmail("jwt@example.com");
        users.register(user);
        var response = login("test-password");
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().allValues("set-cookie").isEmpty());
        var data = mapper.readTree(response.body()).path("data");
        assertEquals("Bearer", data.path("tokenType").asText());
        assertEquals(3600, data.path("expiresIn").asInt());
        token = data.path("accessToken").asText();
        assertEquals(3, token.split("\\.").length);
    }

    HttpResponse<String> login(String password) throws Exception {
        return client.send(HttpRequest.newBuilder(uri("/api/auth/login"))
            .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(
                "{\"username\":\"jwt-reader\",\"password\":\"" + password + "\"}"))
            .build(), HttpResponse.BodyHandlers.ofString());
    }
    URI uri(String path) { return URI.create("http://127.0.0.1:" + port + path); }
    HttpRequest request(String authorization, String id) {
        var builder = HttpRequest.newBuilder(uri("/api/secure/books/" + id)).timeout(Duration.ofSeconds(10));
        if (authorization != null) builder.header("Authorization", authorization);
        return builder.GET().build();
    }
    HttpResponse<String> get(String authorization) throws Exception {
        return client.send(request(authorization, "jwt-book"), HttpResponse.BodyHandlers.ofString());
    }
    void assertUnauthorized(HttpResponse<String> response) throws Exception {
        assertEquals(401, response.statusCode());
        assertEquals("Bearer", response.headers().firstValue("www-authenticate").orElseThrow());
        assertEquals(40101, mapper.readTree(response.body()).path("code").asInt());
        assertTrue(response.headers().allValues("set-cookie").isEmpty());
    }

    @Test void rejectsMissingTokenBeforeResourceLookup() throws Exception {
        assertUnauthorized(get(null));
        assertUnauthorized(client.send(request(null, "missing"), HttpResponse.BodyHandlers.ofString()));
    }
    @Test void rejectsMalformedWrongSchemeAndTamperedTokens() throws Exception {
        String[] parts = token.split("\\.");
        String signature = (parts[2].charAt(0) == 'A' ? "B" : "A") + parts[2].substring(1);
        for (String value : List.of("Bearer broken", "Basic " + token, "Bearer",
                "Bearer " + parts[0] + "." + parts[1] + "." + signature)) {
            assertUnauthorized(get(value));
        }
    }
    @Test void rejectsExpiredWrongIssuerAndMissingExpiration() throws Exception {
        String expired = Jwts.builder().issuer("bookstore").subject("jwt-reader")
            .expiration(Date.from(Instant.now().minusSeconds(60))).signWith(KEY).compact();
        String issuer = Jwts.builder().issuer("another-service").subject("jwt-reader")
            .expiration(Date.from(Instant.now().plusSeconds(60))).signWith(KEY).compact();
        String noExpiry = Jwts.builder().issuer("bookstore").subject("jwt-reader").signWith(KEY).compact();
        for (String value : List.of(expired, issuer, noExpiry)) assertUnauthorized(get("Bearer " + value));
    }
    @Test void returnsBookOnRealVirtualThreadWithoutSession() throws Exception {
        var response = get("Bearer " + token);
        assertEquals(200, response.statusCode());
        assertEquals("jwt-book", mapper.readTree(response.body()).path("data").path("id").asText());
        assertEquals("true", response.headers().firstValue("x-thread-virtual").orElseThrow());
        assertFalse(response.headers().firstValue("x-thread-name").orElseThrow().isBlank());
        assertTrue(response.headers().allValues("set-cookie").isEmpty());
        assertUnauthorized(get(null)); // 同一个启用 Cookie 的客户端不会继承登录状态。
        assertTrue(cookies.getCookieStore().getCookies().isEmpty());
    }
    @Test void validTokenStillHonorsNotFound() throws Exception {
        var response = client.send(request("Bearer " + token, "missing"), HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }
    @Test void rejectsIncorrectPassword() throws Exception { assertEquals(401, login("incorrect").statusCode()); }
    @Test void permitsCorsPreflightWithAuthorizationHeader() throws Exception {
        var response = client.send(HttpRequest.newBuilder(uri("/api/secure/books/jwt-book"))
            .header("Origin", "http://127.0.0.1:5173").header("Access-Control-Request-Method", "GET")
            .header("Access-Control-Request-Headers", "authorization").method("OPTIONS", HttpRequest.BodyPublishers.noBody())
            .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("http://127.0.0.1:5173", response.headers().firstValue("access-control-allow-origin").orElseThrow());
    }
    @Test void handlesConcurrentSlowQueriesOnVirtualThreads() throws Exception {
        var calls = IntStream.range(0, 32).mapToObj(i -> client.sendAsync(request("Bearer " + token, "jwt-book"),
            HttpResponse.BodyHandlers.ofString())).toList();
        CompletableFuture.allOf(calls.toArray(CompletableFuture[]::new)).get(15, TimeUnit.SECONDS);
        for (var call : calls) {
            assertEquals(200, call.join().statusCode());
            assertEquals("true", call.join().headers().firstValue("x-thread-virtual").orElseThrow());
        }
    }
    @Test void sharedKeyWorksAcrossInstancesAndRandomKeysDoNot() {
        var user = new com.homework.bookstore.dto.UserResponse(); user.setUsername("reader"); user.setId(1L);
        String secret = Encoders.BASE64.encode(KEY.getEncoded());
        String shared = new JwtTokenService(secret, 60).issue(user);
        assertEquals("reader", new JwtTokenService(secret, 60).verify(shared));
        assertThrows(io.jsonwebtoken.JwtException.class, () -> new JwtTokenService("", 60).verify(shared));
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenService(secret, 0));
        assertThrows(io.jsonwebtoken.security.WeakKeyException.class, () -> new JwtTokenService("YWJj", 60));
    }
}
