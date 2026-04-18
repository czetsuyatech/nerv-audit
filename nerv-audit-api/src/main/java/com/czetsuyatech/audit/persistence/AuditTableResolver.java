package com.czetsuyatech.audit.persistence;

import java.util.Optional;

public interface AuditTableResolver {
    Optional<String> resolve(String entityName);
}
