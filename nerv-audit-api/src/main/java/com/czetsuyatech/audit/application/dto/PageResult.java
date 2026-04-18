package com.czetsuyatech.audit.application.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

  private List<T> content;
  private long total;
  private int offset = 0;
  private int limit = 50;

  public PageResult(List<T> content) {
    this.content = content;
  }

  public static <T> PageResult<T> of(List<T> content) {
    return new PageResult<T>(content);
  }
}
