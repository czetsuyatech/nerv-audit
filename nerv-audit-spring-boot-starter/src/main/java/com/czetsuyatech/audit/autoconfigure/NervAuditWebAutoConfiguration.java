package com.czetsuyatech.audit.autoconfigure;

import com.czetsuyatech.audit.infrastructure.license.LicenseService;
import com.czetsuyatech.audit.persistence.repository.AuditRepository;
import com.czetsuyatech.audit.service.AuditService;
import com.czetsuyatech.audit.web.controller.AuditController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnProperty(
    prefix = "nerv.audit.web",
    name = "enabled",
    havingValue = "true"
)
public class NervAuditWebAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnBean(AuditRepository.class)
  public AuditController auditController(AuditService auditService, LicenseService licenseService) {
    return new AuditController(auditService);
  }
}
