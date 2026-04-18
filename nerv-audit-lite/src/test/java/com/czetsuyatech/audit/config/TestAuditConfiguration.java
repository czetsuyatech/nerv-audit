package com.czetsuyatech.audit.config;

import com.czetsuyatech.audit.infrastructure.envers.AuditStrategyType;
import com.czetsuyatech.audit.infrastructure.envers.listener.NervEnversListenerConfigurer;
import com.czetsuyatech.audit.infrastructure.license.License;
import com.czetsuyatech.audit.infrastructure.license.LicenseService;
import com.czetsuyatech.audit.persistence.AuditSqlBuilder;
import com.czetsuyatech.audit.persistence.AuditTableResolver;
import jakarta.persistence.EntityManagerFactory;
import java.util.Locale;
import java.util.Optional;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Test-only bean configuration that provides audit infrastructure beans not available via component scan (since they
 * have no @Component annotation in the main module).
 *
 * <p>AuditRepository and AuditServiceImpl are picked up via @Repository/@Service
 * component scanning from the TestApplication context.</p>
 */
@TestConfiguration
public class TestAuditConfiguration {

  @Bean
  public AuditConfig auditConfig() {
    return AuditConfig.builder()
        .auditStrategyType(AuditStrategyType.VERTICAL)
        .auditInsert(false)
        .build();
  }

  @Bean
  public AuditSqlBuilder auditSqlBuilder() {
    return new AuditSqlBuilder();
  }

  /**
   * Resolves entity class names to audit table names. Maps "UserEntity" (simple name) → "user_account_aud".
   */
  @Bean
  public AuditTableResolver auditTableResolver() {
    return entityName -> {
      if (entityName == null) {
        return Optional.empty();
      }
      String simple = entityName.contains(".")
          ? entityName.substring(entityName.lastIndexOf('.') + 1)
          : entityName;
      if ("UserEntity".equalsIgnoreCase(simple)) {
        return Optional.of("user_account_aud");
      }
      String base = simple.toLowerCase(Locale.ROOT).replace("entity", "");
      return Optional.of(base + "_aud");
    };
  }

  @Bean
  public LicenseService licenseService() {
    return LicenseService.of(License
        .builder()
        .valid(true)
        .build());
  }

  @Bean
  public NervEnversListenerConfigurer nervEnversListenerConfigurer(
      EntityManagerFactory entityManagerFactory,
      AuditConfig auditConfig,
      LicenseService licenseService
  ) {
    return new NervEnversListenerConfigurer(entityManagerFactory, auditConfig, licenseService);
  }
}
