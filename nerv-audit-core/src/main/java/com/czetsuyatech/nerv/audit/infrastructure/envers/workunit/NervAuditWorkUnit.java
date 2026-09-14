package com.czetsuyatech.nerv.audit.infrastructure.envers.workunit;

import static com.czetsuyatech.nerv.audit.infrastructure.envers.AuditConstant.AUDIT_UPDATED;
import static com.czetsuyatech.nerv.audit.infrastructure.envers.AuditConstant.AUDIT_UPDATED_BY;

import com.czetsuyatech.nerv.audit.persistence.AuditTimestampConverter;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
/**
 * Provides the NervAuditWorkUnit implementation.
 */
public class NervAuditWorkUnit {

  private static final String VERTICAL_INSERT_SQL = """
      INSERT INTO %s
      (id, %s, %s, field_name, old_value, new_value, updated_by, updated)
      VALUES 
      (:id, :revisionType, :revisionId, :fieldName, :oldValue, :newValue, :updatedBy, :updated) 
      """;
  private final Clock clock;
  private final EnversService enversService;
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
    this(enversService, entityName, auditFieldsValues, revisionType, Clock.systemUTC());
  }

  NervAuditWorkUnit(
      EnversService enversService,
      String entityName,
      Map<String, Object> auditFieldsValues,
      RevisionType revisionType,
      Clock clock
  ) {
    this.clock = Objects.requireNonNull(clock, "clock");
    this.enversService = Objects.requireNonNull(enversService, "enversService");
    this.entityName = Objects.requireNonNull(entityName, "entityName");
    this.auditFieldsValues = auditFieldsValues;
    this.revisionType = Objects.requireNonNull(revisionType, "revisionType");
    this.revisionTypePropName = enversService.getConfig().getRevisionTypePropertyName();
    this.revisionFieldName = enversService.getConfig().getRevisionFieldName();
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

    log.debug("perform for={}, fieldName={}, oldValue={}, newValue={}", entityName, fieldName, oldValue, newValue);

    final SessionFactoryImplementor sfi = sessionImplementor.getSessionFactory();
    final NativeQuery<?> query = sessionImplementor.createNativeQuery(
        getVerticalTableInsert(sfi.getMappingMetamodel().getEntityDescriptor(
            auditTableName != null ? auditTableName : enversService.getConfig().getAuditEntityName(entityName))
            .getTableName()));
    // Sequence-backed revision inserts may still be queued when this native INSERT runs.
    // Synchronize its query space so the revision FK is valid even with native Hibernate bootstrapping.
    query.addSynchronizedEntityName(enversService.getConfig().getRevisionInfo().getRevisionInfoEntityName());
    final long safeId = (id != null) ? id : 0L;
    final String fieldNameUpper = (fieldName == null)
        ? null
        : fieldName.toUpperCase(Locale.ROOT);
    final String updatedBy = resolveUpdatedBy();
    final Instant updated = resolveUpdated();

    query.setParameter("id", safeId);
    query.setParameter("revisionType", revisionType.ordinal());
    query.setParameter("revisionId", ((DefaultRevisionEntity) revisionData).getId());
    query.setParameter("fieldName", fieldNameUpper, StandardBasicTypes.STRING);
    query.setParameter("oldValue", oldValue);
    query.setParameter("newValue", newValue);
    query.setParameter(AUDIT_UPDATED_BY, updatedBy, StandardBasicTypes.STRING);
    query.setParameter(AUDIT_UPDATED, updated, StandardBasicTypes.INSTANT);

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

  private Instant resolveUpdated() {

    if (auditFieldsValues != null) {
      final Object v = auditFieldsValues.get(AUDIT_UPDATED.toUpperCase(Locale.ROOT));
      if (v != null) {
        return AuditTimestampConverter.toInstant(v, "vertical insert entity=" + entityName + ", column=updated");
      }
    }

    return Instant.now(clock);
  }

  private String getVerticalTableInsert(String auditTableName) {
    return String.format(VERTICAL_INSERT_SQL, auditTableName, revisionTypePropName, revisionFieldName);
  }
}
