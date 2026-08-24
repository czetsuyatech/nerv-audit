package com.czetsuyatech.nerv.audit.infrastructure.envers.workunit;

import com.czetsuyatech.nerv.audit.infrastructure.envers.AuditStrategyType;
import com.czetsuyatech.nerv.audit.infrastructure.envers.util.NervAuditUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.synchronization.SessionCacheCleaner;
import org.hibernate.envers.internal.synchronization.work.AuditWorkUnit;
import org.hibernate.envers.internal.synchronization.work.WorkUnitMergeVisitor;
import org.hibernate.envers.internal.tools.EntityTools;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.EntityType;

@Slf4j
/**
 * Provides the NervModWorkUnit implementation.
 */
public class NervModWorkUnit extends org.hibernate.envers.internal.synchronization.work.ModWorkUnit implements
    NervAuditPerformer {

  private final Map<String, Object> data;
  private final boolean changes;
  private final EntityPersister entityPersister;
  private final Object[] oldState;
  private final Object[] newState;
  private final Map<Integer, String> dirtyProperties;
  private final AuditStrategyType auditStrategyType;
  private final Map<String, Object> auditFieldsValues;
  private final Set<String> auditFieldsUppercase;
  private final SessionCacheCleaner sessionCacheCleaner = new SessionCacheCleaner();
  private final NervAuditWorkUnit auditWorkUnit;

  public NervModWorkUnit(
      SharedSessionContractImplementor sessionImplementor,
      String entityName,
      EnversService enversService,
      Object id,
      EntityPersister entityPersister,
      Object[] newState,
      Object[] oldState,
      Map<Integer, String> dirtyProperties,
      AuditStrategyType auditStrategyType,
      Map<String, Object> auditFieldsValues) {

    super(sessionImplementor, entityName, enversService, id, entityPersister, newState, oldState);

    log.debug("constructor for={}, id={}", entityName, id);

    this.auditFieldsValues = auditFieldsValues != null ? auditFieldsValues : Map.of();
    this.auditFieldsUppercase = this.auditFieldsValues.keySet().stream()
        .map(k -> k == null ? null : k.toUpperCase())
        .collect(Collectors.toUnmodifiableSet());
    this.auditStrategyType = auditStrategyType;
    this.dirtyProperties = dirtyProperties != null ? dirtyProperties : Map.of();
    this.entityPersister = entityPersister;
    this.oldState = oldState;
    this.newState = newState;
    this.data = new HashMap<>();
    this.changes = enversService.getEntitiesConfigurations()
        .get(getEntityName())
        .getPropertyMapper()
        .map(sessionImplementor, data, entityPersister.getPropertyNames(), newState, oldState);
    this.auditWorkUnit = new NervAuditWorkUnit(enversService, entityName, this.auditFieldsValues, getRevisionType());
  }

  @Override
  public void perform(SharedSessionContractImplementor sessionImplementor, Object revisionData) {

    log.debug("perform started for={}, id={}", getEntityName(), id);

    delegatePerform(new NervAuditContext(
        auditStrategy,
        auditStrategyType,
        sessionImplementor,
        getEntityName(),
        enversService.getConfig(),
        id,
        revisionData,
        this::setPerformed));
  }


  @Override
  public void performVerticalAudit(SharedSessionContractImplementor sessionImplementor, Object revisionData,
      Map<String, Object> data) {
    if (dirtyProperties.isEmpty()) {
      sessionCacheCleaner.scheduleAuditDataRemoval(sessionImplementor.unwrap(Session.class), data);
      return;
    }

    final Long entityId = NervAuditUtil.toLongId(id);
    final Session session = sessionImplementor.unwrap(Session.class);

    for (Entry<Integer, String> dirtyProperty : dirtyProperties.entrySet()) {
      final int idx = dirtyProperty.getKey();
      final String propName = dirtyProperty.getValue();

      if (propName == null || auditFieldsUppercase.contains(propName.toUpperCase())) {
        continue;
      }

      final Object oldVal = oldState != null && idx >= 0 && idx < oldState.length
          ? oldState[idx]
          : null;
      final Object newVal = newState != null && idx >= 0 && idx < newState.length
          ? newState[idx]
          : null;
      if (oldVal == null && newVal == null) {
        continue;
      }

      if (idx >= entityPersister.getPropertyTypes().length) {
        continue;
      }
      final boolean isEntityType = entityPersister.getPropertyTypes()[idx].isEntityType();
      if (isEntityType) {
        auditWorkUnit.perform(
            sessionImplementor,
            revisionData,
            entityId,
            propName,
            getKeyFromEntity(session, idx, oldState),
            getKeyFromEntity(session, idx, newState),
            null);

      } else {
        auditWorkUnit.perform(
            sessionImplementor,
            revisionData,
            entityId,
            propName,
            getStringValueFromObject(oldVal),
            getStringValueFromObject(data.get(propName)),
            null);
      }
    }

    sessionCacheCleaner.scheduleAuditDataRemoval(session, data);
  }

  @Override
  public AuditWorkUnit dispatch(WorkUnitMergeVisitor first) {

    if (first instanceof NervModWorkUnit modWorkUnit) {
      return modWorkUnit.mergeCustom(this);

    } else if (first instanceof NervAddWorkUnit addWorkUnit) {
      return addWorkUnit.mergeCustom(this);

    } else if (first instanceof NervDelWorkUnit) {
      return null;
    }

    return first.merge(this);
  }

  @Override
  public Map<String, Object> generateData(Object revisionData) {
    super.generateData(revisionData);
    fillDataWithId(data, revisionData);
    return data;
  }

  @Override
  public Map<String, Object> getData() {
    return data;
  }

  @Override
  public boolean containsWork() {
    return changes;
  }

  public AuditWorkUnit mergeCustom(NervDelWorkUnit second) {
    return second;
  }

  public AuditWorkUnit mergeCustom(NervModWorkUnit second) {

    final Map<Integer, String> mergedDirty = new HashMap<>(second.dirtyProperties);
    mergedDirty.putAll(this.dirtyProperties);

    return new NervModWorkUnit(
        second.sessionImplementor,
        second.getEntityName(),
        second.enversService,
        second.id,
        second.entityPersister,
        getMergedNewStates(this.newState, second.newState, second.dirtyProperties),
        this.oldState,
        mergedDirty,
        auditStrategyType,
        auditFieldsValues);
  }

  public Map<Integer, String> getDirtyProperties() {
    return dirtyProperties;
  }


  private String getKeyFromEntity(Session session, int propertyIndex, Object[] state) {

    if (state == null
        || propertyIndex < 0
        || propertyIndex >= state.length) {
      return null;
    }

    final String associatedEntityName =
        ((EntityType) entityPersister.getPropertyTypes()[propertyIndex]).getAssociatedEntityName();

    final Object identifier = EntityTools.getIdentifier(
        (SessionImplementor) session,
        associatedEntityName,
        state[propertyIndex]);

    return getStringValueFromObject(identifier);
  }

  private String getStringValueFromObject(Object value) {
    return value != null ? String.valueOf(value) : null;
  }

  private static Object[] getMergedNewStates(
      Object[] newState,
      Object[] secondNewState,
      Map<Integer, String> secondDirtyProperties) {

    if (newState == null
        || secondNewState == null
        || newState.length != secondNewState.length) {
      throw new IllegalArgumentException(
          "Cannot merge work units: newState arrays must be non-null and equal length");
    }

    if (secondDirtyProperties == null || secondDirtyProperties.isEmpty()) {
      return newState;
    }

    final Object[] merged = newState.clone();
    for (Integer idx : secondDirtyProperties.keySet()) {
      if (idx != null && idx >= 0 && idx < merged.length) {
        merged[idx] = secondNewState[idx];
      }
    }
    return merged;
  }
}
