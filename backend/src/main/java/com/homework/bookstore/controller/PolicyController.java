package com.homework.bookstore.controller;
import com.fasterxml.jackson.databind.JsonNode;
import com.homework.bookstore.dto.ApiResponse;
import com.homework.bookstore.dto.PolicySearchRequest;
import com.homework.bookstore.service.PolicyRagClient;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/policies")
public class PolicyController {
    private final PolicyRagClient policies;
    public PolicyController(PolicyRagClient policies) { this.policies=policies; }
    @GetMapping("/status") public ApiResponse<JsonNode> status() { return ApiResponse.success(policies.status()); }
    @GetMapping("/document") public ApiResponse<JsonNode> document() { return ApiResponse.success(policies.document()); }
    @PostMapping("/search") public ApiResponse<JsonNode> search(@Valid @RequestBody PolicySearchRequest request) { return ApiResponse.success(policies.query(request.query().trim())); }
}
