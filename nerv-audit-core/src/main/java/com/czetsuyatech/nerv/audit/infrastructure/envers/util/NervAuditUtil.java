package com.czetsuyatech.nerv.audit.infrastructure.envers.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for NERV | Audit infrastructure.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NervAuditUtil {

  /**
   * Converts an object to a Long ID.
   *
   * @param id the object to convert
   * @return the converted Long, or null if id is null
   */
  public static Long toLongId(Object id) {

    if (id == null) {
      return null;
    }
    if (id instanceof Long l) {
      return l;
    }
    if (id instanceof Number n) {
      return n.longValue();
    }

    return Long.valueOf(String.valueOf(id));
  }
}
