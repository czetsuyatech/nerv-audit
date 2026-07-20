package com.czetsuyatech.nerv.audit.infrastructure.license;

public interface LicenseValidator {

  License validate(String token);
}
