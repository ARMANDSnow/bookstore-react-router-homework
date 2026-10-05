package com.homework.bookstore.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record AssistantReply(String answer, String model, List<Step> steps, List<BookDto> books, List<String> modelRequestIds) {
    public record Step(int index, String name, JsonNode arguments, JsonNode observation, long durationMillis) {}
}
