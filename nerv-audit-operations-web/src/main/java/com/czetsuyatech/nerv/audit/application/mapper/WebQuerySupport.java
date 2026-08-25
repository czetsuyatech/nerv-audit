package com.czetsuyatech.nerv.audit.application.mapper;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * HTTP query parsing shared by the audit operations controllers.
 */
public final class WebQuerySupport {

  public static final int DEFAULT_PAGE_SIZE = 20;
  public static final int MAX_PAGE_SIZE = 100;

  private WebQuerySupport() {
  }

  public static Instant instant(String value, String name) {
    if (value == null) {
      return null;
    }
    try {
      return Instant.parse(value);
    } catch (DateTimeParseException exception) {
      throw new IllegalArgumentException(name + " must be an ISO-8601 instant", exception);
    }
  }

  public static int pageNumber(int page) {
    if (page < 0) {
      throw new IllegalArgumentException("page must not be negative");
    }
    return page;
  }

  public static int pageSize(int size) {
    if (size < 1 || size > MAX_PAGE_SIZE) {
      throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
    }
    return size;
  }
}
