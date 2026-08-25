package com.czetsuyatech.nerv.audit.web.advice;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converts invalid operations query input into a stable HTTP response.
 */
@RestControllerAdvice
public class AuditOperationsWebExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, String> invalidQuery(IllegalArgumentException exception) {
    return Map.of(
        "code", "NERV_AUDIT_INVALID_QUERY",
        "message", exception.getMessage()
    );
  }
}
