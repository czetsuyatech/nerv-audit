package com.czetsuyatech.nerv.audit.autoconfigure;

import com.czetsuyatech.nerv.audit.infrastructure.envers.AuditStrategyType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Core NERV Audit configuration.
 */
@Data
@ConfigurationProperties(prefix = "nerv.audit")
public class AuditProperties {

  private AuditStrategyType auditStrategyType;
  private Boolean auditInsert;
  private String auditFields;
  private Vertical vertical = new Vertical();

  @Data
  public static class Vertical {
    private SchemaValidation schemaValidation = new SchemaValidation();
  }

  @Data
  public static class SchemaValidation {
    private boolean enabled = true;
  }
}
