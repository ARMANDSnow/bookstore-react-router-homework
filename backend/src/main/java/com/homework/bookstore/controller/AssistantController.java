package com.homework.bookstore.controller;

import com.homework.bookstore.dto.ApiResponse;
import com.homework.bookstore.dto.AssistantReply;
import com.homework.bookstore.dto.AssistantRequest;
import com.homework.bookstore.service.AssistantService;
import com.homework.bookstore.service.DeepSeekClient;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {
    private final AssistantService assistant;
    private final DeepSeekClient model;
    public AssistantController(AssistantService assistant, DeepSeekClient model) { this.assistant = assistant; this.model = model; }
    @GetMapping("/status")
    public ApiResponse<java.util.Map<String,Object>> status() {
        return ApiResponse.success(java.util.Map.of("configured",model.configured(),"model",model.model()));
    }
    /*
     * 【作业2 · 得分点2.2／2.4／2.5：前后端连接入口】
     * 前端提交本轮问题、受限历史和可选演示场景；@Valid先执行DTO中的长度、角色及场景白名单校验。
     * AssistantService完成真实模型与本地工具循环后，一并返回自然语言answer、工具steps和图书信息。
     * 控制器不自行编写回答或伪造工具结果；API密钥由后端DeepSeekClient读取，浏览器不接触密钥。
     */
    @PostMapping("/chat")
    public ApiResponse<AssistantReply> chat(@Valid @RequestBody AssistantRequest request) {
        return ApiResponse.success(assistant.chat(request));
    }
}
