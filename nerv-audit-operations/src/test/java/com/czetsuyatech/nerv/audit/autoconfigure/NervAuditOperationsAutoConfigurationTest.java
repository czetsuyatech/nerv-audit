package com.czetsuyatech.nerv.audit.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.czetsuyatech.nerv.audit.operations.AuditOperations;
import com.czetsuyatech.nerv.audit.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class NervAuditOperationsAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(NervAuditOperationsAutoConfiguration.class));

  @Test
  void createsTheOperationsFacadeOnlyWhenCoreAuditServiceIsAvailable() {
    contextRunner.run(context -> assertThat(context).doesNotHaveBean(AuditOperations.class));

    contextRunner.withBean(AuditService.class, () -> mock(AuditService.class)).run(context ->
        assertThat(context).hasSingleBean(AuditOperations.class)
    );
  }
}
