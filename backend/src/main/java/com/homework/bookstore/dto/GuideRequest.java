package com.homework.bookstore.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public record GuideRequest(@NotBlank(message="请输入选书或政策问题") @Size(max=1200,message="问题最多1200字") String message,
                           @Size(max=20,message="历史消息过长") List<@NotNull @Valid Turn> history) {
 public record Turn(@Pattern(regexp="user|assistant",message="历史只允许用户与助手消息") @NotBlank String role,
                    @NotBlank @Size(max=4000,message="历史消息最多4000字") String content) {}
}
