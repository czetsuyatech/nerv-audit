package com.czetsuyatech.audit.autoconfigure;

import com.czetsuyatech.audit.infrastructure.envers.AuditStrategyType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "nerv.audit")
public class AuditProperties {

  private AuditStrategyType auditStrategyType;
  private Boolean auditInsert;
  private String auditFields;
}
