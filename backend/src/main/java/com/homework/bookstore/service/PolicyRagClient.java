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
    // 【作业3 · 得分点3.2：把向量检索接入Java工具】
    // query_store_policy最终调用这里，向本地Embedding服务发送自然语言query。
    // /query返回匹配状态、原文片段、policy-N编号及相似度，原样作为Observation
    // 交回代理；本客户端不生成政策答案，也不把检索失败补成自编条款。
    public JsonNode query(String query) { return call("/query", Map.of("query", query)); }
    private JsonNode call(String path, Object body) {
        // 最多4个并发调用，连接上限3秒、单请求上限30秒。非200或非JSON对象
        // 都统一视为服务不可用，由GuideAgentService转成可观察的工具失败结果。
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
