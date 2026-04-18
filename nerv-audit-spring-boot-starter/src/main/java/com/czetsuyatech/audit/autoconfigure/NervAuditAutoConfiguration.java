package com.czetsuyatech.audit.autoconfigure;

import com.czetsuyatech.audit.config.AuditConfig;
import com.czetsuyatech.audit.infrastructure.envers.listener.NervEnversListenerConfigurer;
import com.czetsuyatech.audit.infrastructure.license.LicenseService;
import com.czetsuyatech.audit.persistence.AuditSqlBuilder;
import com.czetsuyatech.audit.persistence.AuditTableResolver;
import com.czetsuyatech.audit.persistence.repository.AuditRepository;
import com.czetsuyatech.audit.service.AuditService;
import com.czetsuyatech.audit.service.AuditServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@Slf4j
@AutoConfiguration
@ConditionalOnClass({AuditReaderFactory.class, EntityManagerFactory.class})
@EnableConfigurationProperties(AuditProperties.class)
public class NervAuditAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public AuditConfig auditConfig(AuditProperties properties) {
    return AuditConfig.builder()
        .auditInsert(properties.getAuditInsert())
        .auditStrategyType(properties.getAuditStrategyType())
        .auditFields(properties.getAuditFields())
        .build();
  }

  @Bean
  @ConditionalOnMissingBean
  public AuditSqlBuilder auditSqlBuilder() {
    return new AuditSqlBuilder();
  }

  @Bean
  @ConditionalOnMissingBean
  public AuditRepository auditRepository(
      EntityManager entityManager,
      AuditSqlBuilder queryBuilder,
      AuditTableResolver auditTableResolver
  ) {
    return new AuditRepository(entityManager, queryBuilder, auditTableResolver);
  }

  @Bean
  @ConditionalOnMissingBean
  public AuditService auditService(AuditRepository auditRepository, EntityManager entityManager) {
    return new AuditServiceImpl(auditRepository, entityManager);
  }

  @Bean
  @ConditionalOnMissingBean
  public NervEnversListenerConfigurer auditNervEnversListenerConfigurer(
      EntityManagerFactory emf,
      AuditConfig auditConfig,
      LicenseService licenseService
  ) {
    return new NervEnversListenerConfigurer(emf, auditConfig, licenseService);
  }

  @Bean
  @ConditionalOnMissingBean
  public AuditTableResolver defaultResolver(EntityManagerFactory emf) {
    return entityName -> {
      try {
        SessionFactoryImplementor sf = emf.unwrap(SessionFactoryImplementor.class);
        EntityPersister persister = sf.getMappingMetamodel().getEntityDescriptor(entityName);

        return Optional.of(persister.getTableName() + "_AUD");

      } catch (Exception e) {
        log.warn("Could not resolve audit table for entity '{}': {}", entityName, e.getMessage());
        return Optional.empty();
      }
    };
  }
}
