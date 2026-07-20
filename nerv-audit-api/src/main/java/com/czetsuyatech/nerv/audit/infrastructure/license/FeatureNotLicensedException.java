package com.czetsuyatech.nerv.audit.infrastructure.license;

public class FeatureNotLicensedException extends RuntimeException {

  public FeatureNotLicensedException(String feature, String reason) {
    super("Feature not licensed: " + feature +
        (reason != null ? " (" + reason + ")" : ""));
  }
}
