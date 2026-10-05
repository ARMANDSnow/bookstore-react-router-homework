package com.homework.bookstore.dto;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
public record GuideReply(String answer,String model,List<Step> steps,List<BookDto> books,List<JsonNode> policyChunks,List<String> modelRequestIds) {
 public record Step(int index,String thought,String action,JsonNode arguments,JsonNode observation,long durationMillis) {}
}
