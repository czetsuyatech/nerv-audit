package com.czetsuyatech.nerv.audit.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;

class AuditTimestampConverterTest {

  @Test
  void normalizesAllSupportedTypesIndependentlyOfDefaultTimezone() {
    TimeZone original = TimeZone.getDefault();
    try {
      TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"));
      Instant expected = Instant.parse("2026-01-15T03:04:05.123Z");
      for (Object value : new Object[] {expected, expected.atOffset(ZoneOffset.ofHours(8)),
          expected.atZone(ZoneId.of("America/New_York")), Timestamp.from(expected), Date.from(expected),
          LocalDateTime.ofInstant(expected, ZoneOffset.UTC)}) {
        assertThat(AuditTimestampConverter.toInstant(value, "test updated")).isEqualTo(expected);
      }
      Instant nanos = Instant.parse("2026-01-15T03:04:05.123456789Z");
      assertThat(AuditTimestampConverter.toInstant(Timestamp.from(nanos), "nanoseconds")).isEqualTo(nanos);
    } finally {
      TimeZone.setDefault(original);
    }
  }

  @Test
  void diagnosesUnsupportedAndMissingValuesWithoutCallingArbitraryToString() {
    Object unsafe = new Object() {
      @Override
      public String toString() {
        throw new AssertionError("must not expose arbitrary data");
      }
    };
    for (Object value : new Object[] {unsafe, "2026-01-01", 42L, java.sql.Date.valueOf("2026-01-01"),
        java.sql.Time.valueOf("12:00:00")}) {
      assertThatThrownBy(() -> AuditTimestampConverter.toInstant(value, "history updated"))
          .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(value.getClass().getName())
          .hasMessageContaining("history updated");
    }
    assertThatThrownBy(() -> AuditTimestampConverter.toInstant(null, "history updated"))
        .hasMessageContaining("null").hasMessageContaining("history updated");
  }
}
