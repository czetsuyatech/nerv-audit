package com.czetsuyatech.audit.infrastructure.envers.workunit;

import com.czetsuyatech.audit.infrastructure.envers.AuditStrategyType;
import java.util.function.Consumer;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.strategy.spi.AuditStrategy;

public record NervAuditContext(
    AuditStrategy auditStrategy,
    AuditStrategyType auditStrategyType,
    SharedSessionContractImplementor sessionImplementer,
    String entityName,
    Configuration configuration,
    Object id,
    Object revision,
    Consumer<Object> performFunc
) {

}
