package com.czetsuyatech.nerv.audit.autoconfigure;

import com.czetsuyatech.nerv.audit.operations.AuditOperations;
import com.czetsuyatech.nerv.audit.web.advice.AuditOperationsWebExceptionHandler;
import com.czetsuyatech.nerv.audit.web.controller.AuditOperationsController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Registers audit inspection endpoints only after explicit opt-in.
 */
@AutoConfiguration(after = NervAuditOperationsAutoConfiguration.class)
@ConditionalOnClass(DispatcherServlet.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "nerv.audit.operations.web", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(NervAuditOperationsWebProperties.class)
public class NervAuditOperationsWebAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnBean(AuditOperations.class)
  public AuditOperationsController auditOperationsController(AuditOperations auditOperations) {
    return new AuditOperationsController(auditOperations);
  }

  @Bean
  @ConditionalOnMissingBean
  public AuditOperationsWebExceptionHandler auditOperationsWebExceptionHandler() {
    return new AuditOperationsWebExceptionHandler();
  }
}
