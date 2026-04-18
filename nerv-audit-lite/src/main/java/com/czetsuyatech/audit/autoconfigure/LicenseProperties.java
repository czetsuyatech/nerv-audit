package com.czetsuyatech.audit.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nerv.audit.license")
@Getter
@Setter
public class LicenseProperties {

  /**
   * Enable/disable license system
   */
  private boolean enabled = true;

  /**
   * License key (JWT)
   */
  private String key;

  /**
   * Public key location in classpath
   */
  private String publicKey = "license/nerv_audit_public_key.pem";
}
