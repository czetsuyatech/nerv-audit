package com.czetsuyatech.nerv.audit.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

/** Converts audit timestamps to absolute instants. Zone-free values denote UTC. */
public final class AuditTimestampConverter {

  private AuditTimestampConverter() {
  }

  public static Instant toInstant(Object value, String context) {
    return switch (value) {
      case Instant instant -> instant;
      case OffsetDateTime offset -> offset.toInstant();
      case ZonedDateTime zoned -> zoned.toInstant();
      case LocalDateTime local -> local.toInstant(ZoneOffset.UTC);
      case Timestamp timestamp -> timestamp.toInstant();
      // SQL DATE and TIME do not describe an absolute moment.
      case java.sql.Date date -> throw unsupported(date, context);
      case java.sql.Time time -> throw unsupported(time, context);
      case Date date -> date.toInstant();
      case null -> throw new IllegalArgumentException("NERV Audit timestamp is null (" + context + ")");
      default -> throw unsupported(value, context);
    };
  }

  private static IllegalArgumentException unsupported(Object value, String context) {
    // Do not invoke arbitrary toString implementations or include possible payloads.
    String safeValue = value instanceof java.sql.Date || value instanceof java.sql.Time
        || value instanceof Number ? ", value=" + value : "";
    return new IllegalArgumentException("NERV Audit unsupported timestamp type "
        + value.getClass().getName() + safeValue + " (" + context + ")");
  }
}
