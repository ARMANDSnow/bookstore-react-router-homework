package com.homework.bookstore.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record PolicySearchRequest(@NotBlank(message="请输入政策问题") @Size(max=600,message="政策问题最多600字") String query) {}
