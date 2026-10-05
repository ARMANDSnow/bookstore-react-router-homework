package com.homework.bookstore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.bookstore.service.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Semaphore;

@Service
public class PolicyRagClient {
    private final ObjectMapper mapper;
    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final Semaphore capacity = new Semaphore(4);
    public PolicyRagClient(ObjectMapper mapper, @Value("${policy.base-url:http://127.0.0.1:8091}") String baseUrl) {
        this.mapper = mapper; this.baseUrl = baseUrl;
    }
    public JsonNode status() { return call("/status", null); }
    public JsonNode document() { return call("/document", null); }
    public JsonNode query(String query) { return call("/query", Map.of("query", query)); }
    private JsonNode call(String path, Object body) {
        if (!capacity.tryAcquire()) throw unavailable();
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(30));
            if (body != null) builder.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw unavailable();
            JsonNode result = mapper.readTree(response.body());
            if (result == null || !result.isObject()) throw unavailable();
            return result;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt(); throw unavailable();
        } catch (Exception exception) { throw unavailable(); }
        finally { capacity.release(); }
    }
    private BusinessException unavailable() { return new BusinessException(50330, "政策检索暂时不可用，请稍后重试。"); }
}
