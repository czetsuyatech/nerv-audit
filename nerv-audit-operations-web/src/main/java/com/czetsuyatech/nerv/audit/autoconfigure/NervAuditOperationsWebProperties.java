package com.czetsuyatech.nerv.audit.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the optional audit operations HTTP surface.
 */
@ConfigurationProperties("nerv.audit.operations.web")
public class NervAuditOperationsWebProperties {

  private String basePath = "/management/nerv-audit";

  public String getBasePath() {
    return basePath;
  }

  public void setBasePath(String basePath) {
    if (basePath == null || basePath.isBlank() || !basePath.startsWith("/")) {
      throw new IllegalArgumentException("nerv.audit.operations.web.base-path must start with '/'");
    }
    this.basePath = basePath.endsWith("/") && basePath.length() > 1
        ? basePath.substring(0, basePath.length() - 1)
        : basePath;
  }
}
