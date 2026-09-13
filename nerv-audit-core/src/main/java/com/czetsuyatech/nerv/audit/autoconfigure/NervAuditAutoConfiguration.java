package com.czetsuyatech.nerv.audit.autoconfigure;

import com.czetsuyatech.nerv.audit.config.AuditConfig;
import com.czetsuyatech.nerv.audit.infrastructure.envers.listener.NervEnversListenerConfigurer;
import com.czetsuyatech.nerv.audit.persistence.AuditSqlBuilder;
import com.czetsuyatech.nerv.audit.persistence.AuditTableResolver;
import com.czetsuyatech.nerv.audit.persistence.repository.AuditRepository;
import com.czetsuyatech.nerv.audit.service.AuditService;
import com.czetsuyatech.nerv.audit.service.AuditServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.Optional;
import com.czetsuyatech.nerv.audit.infrastructure.envers.AuditStrategyType;
import com.czetsuyatech.nerv.audit.persistence.VerticalAuditSchemaValidator;
import org.hibernate.envers.boot.internal.EnversService;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Configures the Hibernate Envers-backed NERV Audit implementation.
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass({AuditReaderFactory.class, EntityManagerFactory.class})
@EnableConfigurationProperties(AuditProperties.class)
public class NervAuditAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public VerticalAuditSchemaValidator verticalAuditSchemaValidator() {
    return new VerticalAuditSchemaValidator();
  }

  @Bean
  @ConditionalOnProperty(prefix = "nerv.audit.vertical.schema-validation", name = "enabled", matchIfMissing = true)
  public SmartInitializingSingleton verticalAuditSchemaValidation(
      EntityManagerFactory entityManagerFactory, AuditConfig config, VerticalAuditSchemaValidator validator,
      AuditTableResolver resolver) {
    return () -> {
      if (config.getAuditStrategyType() == AuditStrategyType.VERTICAL) {
        validator.validate(entityManagerFactory, resolver);
      }
    };
  }

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
  public AuditService auditService(
      AuditRepository auditRepository,
      EntityManager entityManager
  ) {
    return new AuditServiceImpl(auditRepository, entityManager);
  }

  @Bean
  @ConditionalOnMissingBean
  public NervEnversListenerConfigurer auditNervEnversListenerConfigurer(
      EntityManagerFactory entityManagerFactory,
      AuditConfig auditConfig
  ) {
    return new NervEnversListenerConfigurer(entityManagerFactory, auditConfig);
  }

  @Bean
  @ConditionalOnMissingBean
  public AuditTableResolver defaultAuditTableResolver(EntityManagerFactory entityManagerFactory) {
    return entityName -> {
      try {
        SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
        EntityPersister persister = sessionFactory.getMappingMetamodel().getEntityDescriptor(entityName);
        EnversService envers = sessionFactory.getServiceRegistry().getService(EnversService.class);
        if (!envers.getEntitiesConfigurations().isVersioned(persister.getEntityName())) {
          return Optional.empty();
        }
        return Optional.of(sessionFactory.getMappingMetamodel().getEntityDescriptor(
            envers.getConfig().getAuditEntityName(persister.getEntityName())).getTableName());
      } catch (Exception exception) {
        log.warn("Could not resolve audit table for entity '{}': {}", entityName, exception.getMessage());
        return Optional.empty();
      }
    };
  }
}
