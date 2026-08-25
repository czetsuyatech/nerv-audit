package com.czetsuyatech.nerv.audit.application.dto;

import java.util.List;

/**
 * Public JSON page contract, intentionally independent of Spring Data.
 */
public record PageResponse<T>(
    List<T> items,
    int page,
    int size,
    long totalElements,
    int totalPages
)
{
  public PageResponse {
    items = List.copyOf(items);
  }
}
