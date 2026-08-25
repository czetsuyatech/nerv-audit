package com.czetsuyatech.nerv.audit.infrastructure.envers.workunit;

import com.czetsuyatech.nerv.audit.infrastructure.envers.AuditStrategyType;
import java.util.Map;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.slf4j.LoggerFactory;

/**
 * Defines the NervAuditPerformer contract.
 */
public interface NervAuditPerformer {

  default void delegatePerform(NervAuditContext auditParam) {

    LoggerFactory.getLogger(NervAuditPerformer.class).debug("delegatePerform for={}", auditParam.entityName());

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
