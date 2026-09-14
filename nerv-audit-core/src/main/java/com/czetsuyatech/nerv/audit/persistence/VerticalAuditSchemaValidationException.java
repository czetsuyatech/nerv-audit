package com.czetsuyatech.nerv.audit.persistence;

import java.util.List;

/** Actionable, immutable schema diagnostics without JDBC metadata exposure. */
public class VerticalAuditSchemaValidationException extends IllegalStateException {

  private final List<String> problems;

  public VerticalAuditSchemaValidationException(List<String> problems) {
    super("NERV Audit vertical schema validation failed:\n- " + String.join("\n- ", problems));
    this.problems = List.copyOf(problems);
  }

  public List<String> getProblems() {
    return problems;
  }
}
