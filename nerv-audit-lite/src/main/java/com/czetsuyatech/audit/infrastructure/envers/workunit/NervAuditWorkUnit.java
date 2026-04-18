package com.czetsuyatech.audit.infrastructure.envers.workunit;

import static com.czetsuyatech.audit.infrastructure.envers.AuditConstant.AUDIT_UPDATED;
import static com.czetsuyatech.audit.infrastructure.envers.AuditConstant.AUDIT_UPDATED_BY;

import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

public class NervAuditWorkUnit {

  private static final String DEFAULT_SCHEMA_PROPERTY = "hibernate.default_schema";
  private static final String VERTICAL_INSERT_SQL = """
      INSERT INTO %s
      (id, %s, %s, field_name, old_value, new_value, updated_by, updated)
      VALUES 
      (:id, :revisionType, :revisionId, :fieldName, :oldValue, :newValue, :updatedBy, :updated) 
      """;
  private transient volatile Class<?> entityClassCache;
  private final EnversService enversService;
  private final String tableName;
  private final String entityName;
  private final String revisionTypePropName;
  private final String revisionFieldName;
  private final Map<String, Object> auditFieldsValues;
  private final RevisionType revisionType;

  NervAuditWorkUnit(
      EnversService enversService,
      String entityName,
      Map<String, Object> auditFieldsValues,
      RevisionType revisionType
  ) {
    this.enversService = Objects.requireNonNull(enversService, "enversService");
    this.entityName = Objects.requireNonNull(entityName, "entityName");
    this.auditFieldsValues = auditFieldsValues;
    this.revisionType = Objects.requireNonNull(revisionType, "revisionType");
    this.revisionTypePropName = enversService.getConfig().getRevisionTypePropertyName();
    this.revisionFieldName = enversService.getConfig().getRevisionFieldName();
    this.tableName = getAuditTableName();
  }

  public void perform(
      SharedSessionContractImplementor sessionImplementor,
      Object revisionData,
      Long id,
      String fieldName,
      String oldValue,
      String newValue,
      String auditTableName
  ) {

    final SessionFactoryImplementor sfi = sessionImplementor.getSessionFactory();
    final String schemaName = (String) sfi.getProperties().get(DEFAULT_SCHEMA_PROPERTY);
    final NativeQuery<?> query = sessionImplementor.createNativeQuery(
        getVerticalTableInsert(schemaName, auditTableName));
    final long safeId = (id != null) ? id : 0L;
    final String fieldNameUpper = (fieldName == null)
        ? null
        : fieldName.toUpperCase(Locale.ROOT);
    final String updatedBy = resolveUpdatedBy();
    final Object updated = resolveUpdated();

    query.setParameter("id", safeId);
    query.setParameter("revisionType", revisionType.ordinal());
    query.setParameter("revisionId", ((DefaultRevisionEntity) revisionData).getId());
    query.setParameter("fieldName", fieldNameUpper, StandardBasicTypes.STRING);
    query.setParameter("oldValue", oldValue);
    query.setParameter("newValue", newValue);
    query.setParameter(AUDIT_UPDATED_BY, updatedBy, StandardBasicTypes.STRING);
    query.setParameter(AUDIT_UPDATED, updated);

    query.executeUpdate();
  }

  private String resolveUpdatedBy() {

    if (auditFieldsValues != null) {
      final Object v = auditFieldsValues.get(AUDIT_UPDATED_BY.toUpperCase(Locale.ROOT));
      if (v != null && StringUtils.hasText(String.valueOf(v))) {
        return String.valueOf(v);
      }
    }

    final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null && StringUtils.hasText(auth.getName())
        ? auth.getName()
        : "SYSTEM";
  }

  private Object resolveUpdated() {

    if (auditFieldsValues != null) {
      final Object v = auditFieldsValues.get(AUDIT_UPDATED.toUpperCase(Locale.ROOT));
      if (v != null) {
        return v;
      }
    }

    return Instant.now();
  }

  private String getVerticalTableInsert(String schemaName, String auditTableName) {

    final String resolvedTable = resolveAuditTableName(schemaName, auditTableName);
    return String.format(VERTICAL_INSERT_SQL, resolvedTable, revisionTypePropName, revisionFieldName);
  }

  private String resolveAuditTableName(String schemaName, String auditTableName) {

    String table = (auditTableName != null) ? auditTableName : tableName;
    if (!StringUtils.hasText(table)) {
      throw new IllegalStateException("Audit table does not exists for entity=" + entityName);
    }

    if (StringUtils.hasText(schemaName) && table.indexOf('.') == -1) {
      table = schemaName + '.' + table;
    }

    return table;
  }

  private <T> T withEntityClass(Function<Class<?>, T> fn) {

    Class<?> c = entityClassCache;
    if (c == null) {
      synchronized (this) {
        c = entityClassCache;
        if (c == null) {
          c = tryLoadEntityClass(entityName)
              .orElse(null);
          entityClassCache = c;
        }
      }
    }

    return (c != null) ? fn.apply(c) : null;
  }

  private Optional<Class<?>> tryLoadEntityClass(String fqcn) {

    try {
      return Optional.of(Class.forName(fqcn));

    } catch (ClassNotFoundException ignored) {
      return Optional.empty();
    }
  }

  private String getSingleEntityNameIntern() {
    return withEntityClass(c -> c.getSimpleName().toUpperCase(Locale.ROOT));
  }

  private String getAuditTableName() {
    return withEntityClass(c -> {
      final Table table = c.getAnnotation(Table.class);
      final String baseTableName = (table != null)
          ? table.name()
          : null;
      return enversService.getConfig().getAuditTableName(entityName, baseTableName);
    });
  }
}
