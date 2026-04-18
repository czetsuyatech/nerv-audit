package com.czetsuyatech.audit.infrastructure.envers.listener;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public interface FieldNormalizer {

  default Set<String> normalizeAuditFields(String[] auditFields) {

    if (auditFields == null || auditFields.length == 0) {
      return Collections.emptySet();
    }
    return Arrays.stream(auditFields)
        .filter(s -> s != null && !s.isBlank())
        .map(s -> s.toUpperCase(Locale.ROOT))
        .collect(Collectors.toUnmodifiableSet());
  }
}
