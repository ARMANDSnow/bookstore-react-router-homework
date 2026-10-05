package com.homework.bookstore.controller;
import com.homework.bookstore.dto.*;
import com.homework.bookstore.service.GuideAgentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/guide")
public class GuideController {
 private final GuideAgentService guide;
 public GuideController(GuideAgentService guide){this.guide=guide;}
 @PostMapping("/chat") public ApiResponse<GuideReply> chat(@Valid @RequestBody GuideRequest request){return ApiResponse.success(guide.chat(request));}
}
