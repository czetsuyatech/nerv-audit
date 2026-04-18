package com.czetsuyatech.audit.infrastructure.license;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public final class License {

  private final boolean valid;
  private final String reason;

  private final String subject;
  private final String type;
  private final List<String> features;
  private final Instant expiry;

  private License(boolean valid,
      String reason,
      String subject,
      String type,
      List<String> features,
      Instant expiry) {

    this.valid = valid;
    this.reason = reason;
    this.subject = subject;
    this.type = type;
    this.features = features != null
        ? Collections.unmodifiableList(features)
        : Collections.emptyList();
    this.expiry = expiry;
  }

  // --- Factory methods ---

  public static License valid(String subject,
      String type,
      List<String> features,
      Long expiryEpochSeconds) {

    Instant expiry = expiryEpochSeconds != null
        ? Instant.ofEpochSecond(expiryEpochSeconds)
        : null;

    return new License(true, null, subject, type, features, expiry);
  }

  public static License invalid(String reason) {
    return new License(false, reason, null, null, List.of(), null);
  }

  // --- State checks ---

  public boolean isValid() {
    return valid;
  }

  public boolean isExpired() {
    return valid && expiry != null && Instant.now().isAfter(expiry);
  }

  public boolean hasFeature(String feature) {
    return valid && features.contains(feature);
  }

  // --- Getters ---

  public String getSubject() {
    return subject;
  }

  public String getType() {
    return type;
  }

  public List<String> getFeatures() {
    return features;
  }

  public Instant getExpiry() {
    return expiry;
  }

  public String getReason() {
    return reason;
  }

  @Override
  public String toString() {
    return "License{" +
        "valid=" + valid +
        ", subject='" + subject + '\'' +
        ", type='" + type + '\'' +
        ", expiry=" + expiry +
        ", reason='" + reason + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof License)) {
      return false;
    }
    License that = (License) o;
    return valid == that.valid &&
        Objects.equals(subject, that.subject) &&
        Objects.equals(type, that.type) &&
        Objects.equals(features, that.features) &&
        Objects.equals(expiry, that.expiry);
  }

  @Override
  public int hashCode() {
    return Objects.hash(valid, subject, type, features, expiry);
  }
}
