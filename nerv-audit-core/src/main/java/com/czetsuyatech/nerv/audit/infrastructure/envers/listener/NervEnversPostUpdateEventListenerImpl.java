package com.czetsuyatech.nerv.audit.infrastructure.envers.listener;

import com.czetsuyatech.nerv.audit.infrastructure.envers.AuditStrategyType;
import com.czetsuyatech.nerv.audit.infrastructure.envers.workunit.NervModWorkUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.event.spi.EnversPostUpdateEventListenerImpl;
import org.hibernate.envers.internal.synchronization.AuditProcess;
import org.hibernate.envers.internal.synchronization.work.AuditWorkUnit;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

@Slf4j
/**
 * Provides the NervEnversPostUpdateEventListenerImpl implementation.
 */
public class NervEnversPostUpdateEventListenerImpl extends EnversPostUpdateEventListenerImpl implements
    FieldNormalizer {

  private final Set<String> auditFields; // upper-cased
  private final AuditStrategyType auditStrategyType;

  public NervEnversPostUpdateEventListenerImpl(
      EnversService enversService,
      AuditStrategyType auditStrategyType,
      String[] auditFields) {

    super(enversService);

    this.auditFields = normalizeAuditFields(auditFields);
    this.auditStrategyType = auditStrategyType;
  }

  @Override
  public void onPostUpdate(PostUpdateEvent event) {

    final EntityPersister persister = event.getPersister();
    final String entityName = persister.getEntityName();

    log.debug("onPostUpdate for={}, id={}", entityName, event.getId());

    if (!getEnversService().getEntitiesConfigurations().isVersioned(entityName)) {
      return;
    }

    final int[] dirty = event.getDirtyProperties();
    if (dirty == null || dirty.length == 0) {
      return;
    }

    checkIfTransactionInProgress(event.getSession());
    final AuditProcess auditProcess = getEnversService().getAuditProcessManager().get(event.getSession());
    final Map<Integer, String> dirtyPropertiesByIndex = new HashMap<>(dirty.length);
    final Map<String, Object> auditFieldValues = initAuditFieldValues();
    final Object[] oldState = getOldStateDB(auditProcess, entityName, event);
    final Object[] newDbState = buildNewDbState(event, persister, auditFieldValues, dirtyPropertiesByIndex);

    if (!shouldCreateWorkUnit(event, persister)) {
      return;
    }

    addWorkUnit(event, oldState, newDbState, auditProcess, entityName, auditFieldValues, dirtyPropertiesByIndex);
  }

  private Map<String, Object> initAuditFieldValues() {

    final Map<String, Object> values = new HashMap<>(Math.max(16, auditFields.size() * 2));
    for (String field : auditFields) {
      values.put(field, null);
    }

    return values;
  }

  private Object[] getOldStateDB(AuditProcess auditProcess, String entityName, PostUpdateEvent event) {

    if (isDetachedEntityUpdate(entityName, event.getOldState())) {
      return auditProcess.getCachedEntityState(event.getId(), entityName);
    }

    return event.getOldState();
  }

  private Object[] buildNewDbState(
      PostUpdateEvent event,
      EntityPersister persister,
      Map<String, Object> auditFieldValues,
      Map<Integer, String> dirtyPropertiesByIndex
  ) {

    final Object[] newDbState = event.getState().clone();

    if (event.getOldState() == null) {
      return newDbState;
    }

    final int[] dirty = event.getDirtyProperties();
    final Set<Integer> dirtyIndexes = new HashSet<>(dirty.length * 2);
    for (int index : dirty) {
      dirtyIndexes.add(index);
    }

    final String[] propertyNames = persister.getPropertyNames();
    final Type[] propertyTypes = persister.getPropertyTypes();

    for (int i = 0; i < propertyNames.length; i++) {
      final String propNameUpper = propertyNames[i].toUpperCase(java.util.Locale.ROOT);

      if (auditFields.contains(propNameUpper)) {
        auditFieldValues.put(propNameUpper, newDbState[i]);
      }

      final boolean isDirty = dirtyIndexes.contains(i);
      final boolean nonAuditedType = isPropertyTypeNotAudited(propertyTypes[i]);

      if (!isDirty || nonAuditedType) {
        newDbState[i] = null;
        continue;
      }

      setNewDbStringNullValue(newDbState, propertyTypes[i], i);
      dirtyPropertiesByIndex.put(i, propertyNames[i]);
    }

    return newDbState;
  }

  private boolean isPropertyTypeNotAudited(Type propertyType) {
    return propertyType.isCollectionType();
  }

  private void setNewDbStringNullValue(Object[] newDbState, Type propertyType, int i) {

    if (AuditStrategyType.HORIZONTAL.equals(auditStrategyType)
        && String.class.equals(propertyType.getReturnedClass())
        && newDbState[i] == null) {
      newDbState[i] = " ";
    }
  }

  private boolean shouldCreateWorkUnit(PostUpdateEvent event, EntityPersister persister) {

    final int[] dirty = event.getDirtyProperties();
    if (dirty == null || dirty.length == 0) {
      return false;
    }

    final String[] propertyNames = persister.getPropertyNames();
    final Type[] propertyTypes = persister.getPropertyTypes();

    for (int index : dirty) {
      final String propNameUpper = propertyNames[index].toUpperCase(java.util.Locale.ROOT);
      if (!auditFields.contains(propNameUpper) && !isPropertyTypeNotAudited(propertyTypes[index])) {
        return true;
      }
    }

    return false;
  }

  private void addWorkUnit(
      PostUpdateEvent event,
      Object[] oldState,
      Object[] newDbState,
      AuditProcess auditProcess,
      String entityName,
      Map<String, Object> auditFieldValues,
      Map<Integer, String> dirtyPropertiesByIndex
  ) {

    final AuditWorkUnit workUnit = new NervModWorkUnit(
        event.getSession(),
        entityName,
        getEnversService(),
        event.getId(),
        event.getPersister(),
        newDbState,
        oldState,
        dirtyPropertiesByIndex,
        auditStrategyType,
        auditFieldValues);

    auditProcess.addWorkUnit(workUnit);
  }
}
