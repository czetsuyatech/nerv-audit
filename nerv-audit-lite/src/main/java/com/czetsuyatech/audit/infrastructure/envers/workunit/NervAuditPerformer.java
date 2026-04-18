package com.czetsuyatech.audit.infrastructure.envers.workunit;

import com.czetsuyatech.audit.infrastructure.envers.AuditStrategyType;
import java.util.Map;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

public interface NervAuditPerformer {

  default void delegatePerform(NervAuditContext auditParam) {

    final Map<String, Object> dataGenerated = generateData(auditParam.revision());

    if (AuditStrategyType.HORIZONTAL.equals(auditParam.auditStrategyType())) {
      auditParam.auditStrategy().perform(
          auditParam.sessionImplementer(),
          auditParam.entityName(),
          auditParam.configuration(),
          auditParam.id(),
          dataGenerated,
          auditParam.revision()
      );

    } else {
      performVerticalAudit(auditParam.sessionImplementer(), auditParam.revision(), dataGenerated);
    }

    auditParam.performFunc().accept(dataGenerated);
  }

  void performVerticalAudit(SharedSessionContractImplementor sessionImplementor, Object revisionData,
      Map<String, Object> dataGenerated);

  Map<String, Object> generateData(Object revisionData);
}
