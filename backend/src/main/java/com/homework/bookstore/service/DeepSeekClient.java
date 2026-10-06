package com.homework.bookstore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.homework.bookstore.service.exception.BusinessException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Credentials stay on the server; never log upstream headers or raw failures. */
@Service
public class DeepSeekClient {
    private final ObjectMapper mapper;
    private final String key, baseUrl, model;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public DeepSeekClient(ObjectMapper mapper, @Value("${deepseek.api-key:}") String key,
                          @Value("${deepseek.base-url:https://api.deepseek.com}") String baseUrl,
                          @Value("${deepseek.model:deepseek-flash}") String model) {
        this.mapper = mapper; this.key = key; this.baseUrl = baseUrl; this.model = model;
    }
    public String model() { return model; }
    public boolean configured() { return !key.isBlank(); }
    public record Completion(JsonNode message, String requestId) {}

    public Completion complete(ArrayNode messages, ArrayNode tools) {
        if (key.isBlank()) throw new BusinessException(40020, "阅读助手暂不可用，请稍后再来");
        try {
            var body = mapper.createObjectNode().put("model", model).put("temperature", 0).put("max_tokens", 1800).put("tool_choice", "auto");
            body.set("messages", messages); body.set("tools", tools);
            body.set("thinking", mapper.createObjectNode().put("type", "disabled"));
            var request = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                    .timeout(Duration.ofSeconds(60)).header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new BusinessException(40021, "助手服务暂不可用，请稍后重试");
            var payload = mapper.readTree(response.body());
            var choice = payload.path("choices").path(0);
            if (!choice.path("finish_reason").asText().matches("stop|tool_calls"))
                throw new BusinessException(40021, "助手回复未完整生成，请稍后重试");
            var message = choice.path("message");
            if (!message.isObject()) throw new BusinessException(40021, "助手回复暂时无法读取，请稍后重试");
            return new Completion(message, payload.path("id").asText());
        } catch (BusinessException ex) { throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt(); throw new BusinessException(40021, "本次查询已中断，请重试");
        } catch (Exception ex) {
            throw new BusinessException(40021, "助手连接失败或等待超时，请稍后重试");
        }
    }
}
