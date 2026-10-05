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
    @PostMapping("/chat")
    public ApiResponse<AssistantReply> chat(@Valid @RequestBody AssistantRequest request) {
        return ApiResponse.success(assistant.chat(request));
    }
}
