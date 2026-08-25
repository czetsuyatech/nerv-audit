package com.czetsuyatech.nerv.audit.config;

import com.czetsuyatech.nerv.audit.infrastructure.envers.AuditStrategyType;
import com.czetsuyatech.nerv.audit.infrastructure.envers.listener.NervEnversListenerConfigurer;
import com.czetsuyatech.nerv.audit.persistence.AuditSqlBuilder;
import com.czetsuyatech.nerv.audit.persistence.AuditTableResolver;
import jakarta.persistence.EntityManagerFactory;
import java.util.Optional;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
  @ConditionalOnMissingBean
  public AuditTableResolver auditTableResolver(EntityManagerFactory emf) {

    return entityName -> {
      try {
        SessionFactoryImplementor sf = emf.unwrap(SessionFactoryImplementor.class);

        // Try to find the entity by simple name or FQN
        EntityPersister persister = null;
        try {
          persister = sf.getMappingMetamodel().getEntityDescriptor(entityName);
        } catch (Exception e) {
          // If not found, try to find by FQN
          String fqn = "com.czetsuyatech.nerv.audit.infrastructure.envers.entity." + entityName;
          try {
            persister = sf.getMappingMetamodel().getEntityDescriptor(fqn);
          } catch (Exception e2) {
            // ignore
          }
        }

        if (persister == null) {
          return Optional.empty();
        }

        return Optional.of(persister.getTableName() + "_AUD");

      } catch (Exception e) {
        return Optional.empty();
      }
    };
  }

  @Bean
  public NervEnversListenerConfigurer nervEnversListenerConfigurer(
      EntityManagerFactory entityManagerFactory,
      AuditConfig auditConfig
  ) {
    return new NervEnversListenerConfigurer(entityManagerFactory, auditConfig);
  }
}
