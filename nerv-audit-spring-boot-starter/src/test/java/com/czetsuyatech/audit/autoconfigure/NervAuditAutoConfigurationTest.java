package com.czetsuyatech.audit.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.czetsuyatech.audit.config.AuditConfig;
import com.czetsuyatech.audit.infrastructure.envers.AuditStrategyType;
import com.czetsuyatech.audit.infrastructure.envers.listener.NervEnversListenerConfigurer;
import com.czetsuyatech.audit.infrastructure.license.LicenseService;
import com.czetsuyatech.audit.persistence.AuditSqlBuilder;
import com.czetsuyatech.audit.persistence.AuditTableResolver;
import com.czetsuyatech.audit.persistence.repository.AuditRepository;
import com.czetsuyatech.audit.service.AuditService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.Optional;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class NervAuditAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(NervAuditAutoConfiguration.class))
      .withBean(EntityManagerFactory.class, () -> {
        EntityManagerFactory emf = mock(EntityManagerFactory.class);
        SessionFactoryImplementor sfi = mock(SessionFactoryImplementor.class);
        ServiceRegistryImplementor registry = mock(ServiceRegistryImplementor.class);

        org.mockito.Mockito.when(emf.unwrap(SessionFactoryImplementor.class))
            .thenReturn(sfi);
        org.mockito.Mockito.when(sfi.getServiceRegistry())
            .thenReturn(registry);
        org.mockito.Mockito.when(registry.getService(EnversService.class))
            .thenReturn(mock(EnversService.class));
        org.mockito.Mockito.when(registry.getService(EventListenerRegistry.class))
            .thenReturn(mock(EventListenerRegistry.class));
        return emf;
      })
      .withBean(EntityManager.class, () -> mock(EntityManager.class))
      .withBean(LicenseService.class, () -> mock(LicenseService.class));

  @Test
  void auditConfigBean_isCreatedWithDefaults() {

    contextRunner.run(ctx -> {
      assertThat(ctx).hasSingleBean(AuditConfig.class);
      AuditConfig config = ctx.getBean(AuditConfig.class);
      assertThat(config.getAuditStrategyType())
          .isEqualTo(AuditStrategyType.VERTICAL);
      assertThat(config.isAuditInsert()).isFalse();
    });
  }

  @Test
  void auditConfigBean_bindsProperies() {

    contextRunner
        .withPropertyValues(
            "nerv.audit.audit-strategy-type=HORIZONTAL",
            "nerv.audit.audit-insert=true",
            "nerv.audit.audit-fields=updatedBy,updated"
        )
        .run(ctx -> {
          assertThat(ctx).hasSingleBean(AuditConfig.class);
          AuditConfig config = ctx.getBean(AuditConfig.class);
          assertThat(config.getAuditStrategyType())
              .isEqualTo(AuditStrategyType.HORIZONTAL);
          assertThat(config.isAuditInsert()).isTrue();
          assertThat(config.getAuditFields())
              .containsExactlyInAnyOrder("updatedBy", "updated");
        });
  }

  @Test
  void auditSqlBuilderBean_isCreated() {

    contextRunner.run(ctx -> assertThat(ctx).hasSingleBean(AuditSqlBuilder.class));
  }

  @Test
  void auditRepositoryBean_isCreated_whenTableResolverPresent() {

    contextRunner
        .withBean(AuditTableResolver.class, () -> entityName -> Optional.empty())
        .run(ctx -> assertThat(ctx).hasSingleBean(AuditRepository.class));
  }

  @Test
  void auditServiceBean_isCreated_whenTableResolverPresent() {

    contextRunner
        .withBean(AuditTableResolver.class, () -> entityName -> Optional.empty())
        .run(ctx -> assertThat(ctx).hasSingleBean(AuditService.class));
  }

  @Test
  void nervEnversListenerConfigurerBean_isCreated() {

    contextRunner.run(ctx -> assertThat(ctx).hasSingleBean(NervEnversListenerConfigurer.class));
  }

  @Test
  void defaultAuditTableResolverBean_isCreated() {

    contextRunner.run(ctx -> assertThat(ctx).hasSingleBean(AuditTableResolver.class));
  }

  @Test
  void auditConfigBean_notCreated_whenUserProvideCustomBean() {

    AuditConfig customConfig = AuditConfig.builder()
        .auditStrategyType(AuditStrategyType.HORIZONTAL)
        .build();
    contextRunner
        .withBean("customAuditConfig", AuditConfig.class, () -> customConfig)
        .run(ctx -> {
          assertThat(ctx).hasSingleBean(AuditConfig.class);
          assertThat(ctx.getBean(AuditConfig.class))
              .isSameAs(customConfig);
        });
  }

  @Test
  void auditSqlBuilderBean_notCreated_whenUserProvideCustomBean() {

    AuditSqlBuilder customBuilder = new AuditSqlBuilder();
    contextRunner
        .withBean("customBuilder", AuditSqlBuilder.class, () -> customBuilder)
        .run(ctx -> {
          assertThat(ctx).hasSingleBean(AuditSqlBuilder.class);
          assertThat(ctx.getBean(AuditSqlBuilder.class))
              .isSameAs(customBuilder);
        });
  }
}
