package com.czetsuyatech.audit.persistence;

public enum SortDirection {
  ASC, DESC;

  public static SortDirection from(String value) {
    return "DESC".equalsIgnoreCase(value) ? DESC : ASC;
  }
}
