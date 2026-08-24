package com.czetsuyatech.nerv.audit.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class NervAuditAutoConfigurationMetadataTest {

  @Test
  void publishesTheCoreAutoConfiguration() throws Exception {
    String resource = "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";

    try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
      assertThat(input).isNotNull();
      assertThat(new String(input.readAllBytes(), StandardCharsets.UTF_8))
          .contains(NervAuditAutoConfiguration.class.getName());
    }
  }
}
