package com.czetsuyatech.audit.config;

import com.czetsuyatech.audit.infrastructure.envers.AuditConstant;
import com.czetsuyatech.audit.infrastructure.envers.AuditStrategyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuditConfig {

  private AuditStrategyType auditStrategyType;
  private Boolean auditInsert;
  private String auditFields;

  public AuditStrategyType getAuditStrategyType() {
    return auditStrategyType != null
        ? auditStrategyType
        : AuditStrategyType.VERTICAL;
  }

  public boolean isAuditInsert() {
    return auditInsert != null
        ? auditInsert
        : false;
  }

  public String[] getAuditFields() {
    return auditFields != null
        ? auditFields.split(",")
        : AuditConstant.getAuditFields();
  }
}
