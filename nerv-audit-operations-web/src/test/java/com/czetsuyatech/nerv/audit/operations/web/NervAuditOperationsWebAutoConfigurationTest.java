package com.czetsuyatech.nerv.audit.operations.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.czetsuyatech.nerv.audit.autoconfigure.NervAuditOperationsWebAutoConfiguration;
import com.czetsuyatech.nerv.audit.operations.AuditOperations;
import com.czetsuyatech.nerv.audit.web.controller.AuditOperationsController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class NervAuditOperationsWebAutoConfigurationTest {

  private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(NervAuditOperationsWebAutoConfiguration.class))
      .withBean(AuditOperations.class, () -> mock(AuditOperations.class));

  @Test
  void endpointsRequireExplicitOptIn() {
    contextRunner.run(context -> assertThat(context).doesNotHaveBean(AuditOperationsController.class));

    contextRunner.withPropertyValues("nerv.audit.operations.web.enabled=true").run(context ->
        assertThat(context).hasSingleBean(AuditOperationsController.class)
    );
  }
}
