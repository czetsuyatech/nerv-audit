package com.czetsuyatech.audit.infrastructure.license;

import java.util.List;

public class LiteLicenseValidator implements LicenseValidator {

  @Override
  public License validate(String token) {
    return License.valid("lite", "LITE", List.of(), null);
  }
}
