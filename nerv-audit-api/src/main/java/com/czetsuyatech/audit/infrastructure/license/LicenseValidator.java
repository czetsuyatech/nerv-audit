package com.czetsuyatech.audit.infrastructure.license;

public interface LicenseValidator {

  License validate(String token);
}
