package com.homework.bookstore.dto;

import java.util.List;

/** 页码从 1 开始，空页仍返回完整分页信息。 */
public record BookPageDto(List<BookDto> items, int page, int size, long totalItems, int totalPages) {}
