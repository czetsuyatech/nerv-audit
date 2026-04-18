package com.czetsuyatech.audit.infrastructure.license;

import java.util.Collections;
import java.util.List;

public class LicenseService {

  private final License license;
  private final boolean enabled;

  private LicenseService(License license, boolean enabled) {
    this.license = license;
    this.enabled = enabled;
  }

  // --- Factory methods ---

  public static LicenseService of(License license) {
    return new LicenseService(license, true);
  }

  public static LicenseService disabled() {
    return new LicenseService(License.invalid("License system disabled"), false);
  }

  // --- State ---

  public boolean isEnabled() {
    return enabled;
  }

  public boolean isValid() {
    return isEnabled() && license.isValid() && !license.isExpired();
  }

  public boolean isExpired() {
    return license.isExpired();
  }

  public String getFailureReason() {
    return license.getReason();
  }

  // --- Feature checks ---

  public boolean hasFeature(String feature) {
    return isValid() && license.hasFeature(feature);
  }

  public void requireFeature(String feature) {
    if (!hasFeature(feature)) {
      throw new FeatureNotLicensedException(
          feature,
          buildReasonMessage(feature)
      );
    }
  }

  private String buildReasonMessage(String feature) {
    if (!enabled) {
      return "License system disabled";
    }

    if (!license.isValid()) {
      return license.getReason();
    }

    if (license.isExpired()) {
      return "License expired";
    }

    return "Feature not included in license";
  }

  // --- Metadata ---

  public String getSubject() {
    return license.getSubject();
  }

  public String getType() {
    return license.getType();
  }

  public List<String> getFeatures() {
    return license.getFeatures() != null
        ? license.getFeatures()
        : Collections.emptyList();
  }

  @Override
  public String toString() {
    return "LicenseService{" +
        "enabled=" + enabled +
        ", valid=" + isValid() +
        ", subject=" + getSubject() +
        ", type=" + getType() +
        '}';
  }
}
