package com.czetsuyatech.audit.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.czetsuyatech.audit.infrastructure.license.LicenseService;
import com.czetsuyatech.audit.persistence.repository.AuditRepository;
import com.czetsuyatech.audit.service.AuditService;
import com.czetsuyatech.audit.web.controller.AuditController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class NervAuditWebAutoConfigurationTest {

  private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(NervAuditWebAutoConfiguration.class));

  @Test
  void auditControllerBean_notCreated_whenWebPropertyDisabled() {

    contextRunner
        .withPropertyValues("nerv.audit.web.enabled=false")
        .withBean(AuditService.class, () -> mock(AuditService.class))
        .withBean(AuditRepository.class, () -> mock(AuditRepository.class))
        .run(ctx -> assertThat(ctx).doesNotHaveBean(AuditController.class));
  }

  @Test
  void auditControllerBean_notCreated_whenWebPropertyMissing() {

    contextRunner
        .withBean(AuditService.class, () -> mock(AuditService.class))
        .withBean(AuditRepository.class, () -> mock(AuditRepository.class))
        .run(ctx -> assertThat(ctx).doesNotHaveBean(AuditController.class));
  }

  @Test
  void auditControllerBean_created_whenWebPropertyEnabled_andAuditRepositoryPresent() {

    contextRunner
        .withPropertyValues("nerv.audit.web.enabled=true")
        .withBean(AuditService.class, () -> mock(AuditService.class))
        .withBean(AuditRepository.class, () -> mock(AuditRepository.class))
        .withBean(LicenseService.class, () -> mock(LicenseService.class))
        .run(ctx -> assertThat(ctx).hasSingleBean(AuditController.class));
  }

  @Test
  void auditControllerBean_notCreated_whenAuditRepositoryBeanMissing() {

    contextRunner
        .withPropertyValues("nerv.audit.web.enabled=true")
        .withBean(AuditService.class, () -> mock(AuditService.class))
        .run(ctx -> assertThat(ctx).doesNotHaveBean(AuditController.class));
  }
}
