package com.czetsuyatech.audit.autoconfigure;

import com.czetsuyatech.audit.infrastructure.license.License;
import com.czetsuyatech.audit.infrastructure.license.LicenseService;
import com.czetsuyatech.audit.infrastructure.license.LicenseValidator;
import com.czetsuyatech.audit.infrastructure.license.LiteLicenseValidator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@AutoConfiguration
@EnableConfigurationProperties(LicenseProperties.class)
public class LicenseAutoConfiguration {

  /**
   * Lite license configuration — activated when nerv-audit-lite is on the classpath.
   * Provides a simple always-valid lite license. Skipped if a LicenseValidator bean is already
   * registered (e.g. by nerv-audit-core's own auto-configuration).
   */
  @Configuration
  @ConditionalOnClass(LiteLicenseValidator.class)
  static class LiteLicenseConfiguration {

    @Bean
    @ConditionalOnMissingBean(LicenseValidator.class)
    public LicenseValidator licenseValidator() {
      return new LiteLicenseValidator();
    }

    @Bean
    @ConditionalOnMissingBean(LicenseService.class)
    public LicenseService licenseService(LicenseProperties properties, LicenseValidator validator) {

      if (!properties.isEnabled()) {
        return LicenseService.disabled();
      }

      License license = validator.validate(properties.getKey());
      return LicenseService.of(license);
    }
  }

  /**
   * Fallback — provides a disabled LicenseService when neither lite nor core is on the classpath.
   */
  @Bean
  @ConditionalOnMissingBean(LicenseService.class)
  public LicenseService licenseServiceFallback() {
    return LicenseService.disabled();
  }
}
