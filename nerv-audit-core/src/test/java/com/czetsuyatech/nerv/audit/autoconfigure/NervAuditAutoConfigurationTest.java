package com.czetsuyatech.nerv.audit.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.czetsuyatech.nerv.audit.config.AuditConfig;
import com.czetsuyatech.nerv.audit.infrastructure.envers.AuditStrategyType;
import com.czetsuyatech.nerv.audit.infrastructure.envers.listener.NervEnversListenerConfigurer;
import com.czetsuyatech.nerv.audit.persistence.AuditSqlBuilder;
import com.czetsuyatech.nerv.audit.persistence.VerticalAuditSchemaValidator;
import com.czetsuyatech.nerv.audit.persistence.AuditTableResolver;
import com.czetsuyatech.nerv.audit.persistence.repository.AuditRepository;
import com.czetsuyatech.nerv.audit.service.AuditService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.TimeZone;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
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
      .withPropertyValues("nerv.audit.vertical.schema-validation.enabled=false")
      .withBean(EntityManagerFactory.class, NervAuditAutoConfigurationTest::entityManagerFactory)
      .withBean(EntityManager.class, () -> mock(EntityManager.class));

  @Test
  void createsTheCoreAuditImplementationThroughExplicitConfiguration() {
    contextRunner.run(context -> {
      assertThat(context).hasSingleBean(AuditConfig.class);
      assertThat(context).hasSingleBean(AuditSqlBuilder.class);
      assertThat(context).hasSingleBean(AuditTableResolver.class);
      assertThat(context).hasSingleBean(AuditRepository.class);
      assertThat(context).hasSingleBean(AuditService.class);
      assertThat(context).hasSingleBean(NervEnversListenerConfigurer.class);
    });
  }

  @Test
  void bindsTheNervAuditProperties() {
    contextRunner.withPropertyValues(
        "nerv.audit.audit-strategy-type=HORIZONTAL",
        "nerv.audit.audit-insert=true",
        "nerv.audit.audit-fields=updatedBy,updated"
    ).run(context -> {
      AuditConfig config = context.getBean(AuditConfig.class);
      assertThat(config.getAuditStrategyType()).isEqualTo(AuditStrategyType.HORIZONTAL);
      assertThat(config.isAuditInsert()).isTrue();
      assertThat(config.getAuditFields()).containsExactly("updatedBy", "updated");
    });
  }

  @Test
  void keepsTheAuditServiceUserOverridable() {
    AuditService customService = mock(AuditService.class);

    contextRunner.withBean(AuditService.class, () -> customService).run(context ->
        assertThat(context.getBean(AuditService.class)).isSameAs(customService)
    );
  }

  @Test
  void validationIsDefaultOnAndUsesTheEffectiveAuditConfig() {
    var validator = mock(VerticalAuditSchemaValidator.class);
    contextRunner.withPropertyValues("nerv.audit.vertical.schema-validation.enabled=true")
        .withBean(VerticalAuditSchemaValidator.class, () -> validator)
        .run(context -> {
          assertThat(context).hasNotFailed();
          org.mockito.Mockito.verify(validator).validate(context.getBean(EntityManagerFactory.class),
              context.getBean(AuditTableResolver.class));
          assertThat(context.getBean(AuditProperties.class).getVertical().getSchemaValidation().isEnabled()).isTrue();
        });
    org.mockito.Mockito.reset(validator);
    contextRunner.withPropertyValues("nerv.audit.vertical.schema-validation.enabled=true")
        .withBean(VerticalAuditSchemaValidator.class, () -> validator)
        .withBean(AuditConfig.class, () -> AuditConfig.builder().auditStrategyType(AuditStrategyType.HORIZONTAL).build())
        .run(context -> {
          assertThat(context).hasNotFailed();
          org.mockito.Mockito.verifyNoInteractions(validator);
        });
  }

  @Test
  void defaultClockUsesUtcRegardlessOfJvmTimezone() {
    TimeZone original = TimeZone.getDefault();
    try {
      for (String zone : new String[] {"Asia/Manila", "Pacific/Honolulu"}) {
        TimeZone.setDefault(TimeZone.getTimeZone(zone));
        contextRunner.run(context -> {
          assertThat(context).hasSingleBean(Clock.class).hasBean("nervAuditClock");
          assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneOffset.UTC);
        });
      }
    } finally {
      TimeZone.setDefault(original);
    }
  }

  @Test
  void applicationClockOverridesTheDefaultClock() {
    contextRunner.withUserConfiguration(ClockConfiguration.class).run(context -> {
      assertThat(context).hasSingleBean(Clock.class).doesNotHaveBean("nervAuditClock");
      assertThat(context.getBean(Clock.class)).isSameAs(ClockConfiguration.CLOCK);
    });
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class ClockConfiguration {
    static final Clock CLOCK = Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Bean
    Clock clock() {
      return CLOCK;
    }
  }

  private static EntityManagerFactory entityManagerFactory() {
    EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);
    SessionFactoryImplementor sessionFactory = mock(SessionFactoryImplementor.class);
    ServiceRegistryImplementor serviceRegistry = mock(ServiceRegistryImplementor.class);
    org.mockito.Mockito.when(entityManagerFactory.unwrap(SessionFactoryImplementor.class)).thenReturn(sessionFactory);
    org.mockito.Mockito.when(sessionFactory.getServiceRegistry()).thenReturn(serviceRegistry);
    org.mockito.Mockito.when(serviceRegistry.getService(EnversService.class)).thenReturn(mock(EnversService.class));
    org.mockito.Mockito.when(serviceRegistry.getService(EventListenerRegistry.class))
        .thenReturn(mock(EventListenerRegistry.class));
    return entityManagerFactory;
  }
}
