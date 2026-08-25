package com.czetsuyatech.nerv.audit.autoconfigure;

import com.czetsuyatech.nerv.audit.operations.AuditOperations;
import com.czetsuyatech.nerv.audit.operations.AuditOperationsImpl;
import com.czetsuyatech.nerv.audit.service.AuditService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Configures the transport-independent audit operations facade.
 */
@AutoConfiguration(after = NervAuditAutoConfiguration.class)
@ConditionalOnBean(AuditService.class)
public class NervAuditOperationsAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(AuditOperations.class)
  public AuditOperations auditOperations(AuditService auditService) {
    return new AuditOperationsImpl(auditService);
  }
}
