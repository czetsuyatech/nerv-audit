package com.czetsuyatech.nerv.audit.persistence;

import java.util.Optional;

public interface AuditTableResolver {
    Optional<String> resolve(String entityName);
}
