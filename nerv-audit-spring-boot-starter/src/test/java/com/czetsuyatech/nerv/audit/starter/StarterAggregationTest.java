package com.czetsuyatech.nerv.audit.starter;

import static org.assertj.core.api.Assertions.assertThat;

import com.czetsuyatech.nerv.audit.autoconfigure.NervAuditAutoConfiguration;
import com.czetsuyatech.nerv.audit.autoconfigure.NervAuditOperationsAutoConfiguration;
import com.czetsuyatech.nerv.audit.autoconfigure.NervAuditOperationsWebAutoConfiguration;
import org.junit.jupiter.api.Test;

class StarterAggregationTest {

  @Test
  void exposesAllStandardAuditAutoConfigurationsTransitively() {
    assertThat(NervAuditAutoConfiguration.class).isNotNull();
    assertThat(NervAuditOperationsAutoConfiguration.class).isNotNull();
    assertThat(NervAuditOperationsWebAutoConfiguration.class).isNotNull();
  }
}
