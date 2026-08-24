package com.czetsuyatech.nerv.audit.infrastructure.envers.workunit;

import com.czetsuyatech.nerv.audit.infrastructure.envers.AuditStrategyType;
import com.czetsuyatech.nerv.audit.infrastructure.envers.exception.MergePersistentCollectionChangeWorkUnitException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.entities.mapper.PersistentCollectionChangeData;
import org.hibernate.envers.internal.synchronization.SessionCacheCleaner;
import org.hibernate.envers.internal.synchronization.work.AuditWorkUnit;
import org.hibernate.envers.internal.synchronization.work.PersistentCollectionChangeWorkUnit;
import org.hibernate.envers.internal.synchronization.work.WorkUnitMergeVisitor;

@Slf4j
/**
 * Provides the NervPersistentCollectionChangeWorkUnit implementation.
 */
public class NervPersistentCollectionChangeWorkUnit extends PersistentCollectionChangeWorkUnit {

  private final List<PersistentCollectionChangeData> collectionChanges;
  private final String referencingPropertyName;
  private final AuditStrategyType auditStrategyType;
  private final List<String> auditFields;
  private final SessionCacheCleaner sessionCacheCleaner = new SessionCacheCleaner();
  private final NervAuditWorkUnit auditWorkUnit;

  public NervPersistentCollectionChangeWorkUnit(
      SessionImplementor sessionImplementor,
      String entityName,
      EnversService enversService,
      PersistentCollection collection,
      CollectionEntry collectionEntry,
      Serializable snapshot,
      Object id,
      String referencingPropertyName,
      AuditStrategyType auditStrategyType,
      List<String> auditFields) {

    super(sessionImplementor, entityName, enversService, collection, collectionEntry, snapshot, id,
        referencingPropertyName);

    log.debug("constructor (complex) for={}, id={}", entityName, id);

    this.referencingPropertyName = referencingPropertyName;
    this.collectionChanges = enversService.getEntitiesConfigurations().get(getEntityName()).getPropertyMapper()
        .mapCollectionChanges(sessionImplementor, referencingPropertyName, collection, snapshot, id);
    this.auditFields = auditFields;
    this.auditStrategyType = auditStrategyType;
    this.auditWorkUnit = new NervAuditWorkUnit(enversService, entityName, null, getRevisionType());
  }

  public NervPersistentCollectionChangeWorkUnit(
      SharedSessionContractImplementor sessionImplementor,
      String entityName,
      EnversService enversService,
      Object id,
      List<PersistentCollectionChangeData> collectionChanges,
      String referencingPropertyName,
      AuditStrategyType auditStrategyType,
      List<String> auditFields) {

    super(sessionImplementor, entityName, enversService, id, collectionChanges, referencingPropertyName);

    log.debug("constructor for={}, id{}", entityName, id);

    this.collectionChanges = collectionChanges;
    this.referencingPropertyName = referencingPropertyName;
    this.auditStrategyType = auditStrategyType;
    this.auditFields = auditFields;
    this.auditWorkUnit = new NervAuditWorkUnit(enversService, entityName, null, getRevisionType());
  }

  @Override
  public void perform(SharedSessionContractImplementor sessionImplementor, Object revisionData) {

    log.debug("perform called for={}", getEntityName());

    final Map<String, AuditRecord> auditRecords = new HashMap<>();
    final String originalIdProp = enversService.getConfig().getOriginalIdPropertyName();
    final String revisionField = enversService.getConfig().getRevisionFieldName();
    final String revTypeProp = enversService.getConfig().getRevisionTypePropertyName();
    final Session session = sessionImplementor.unwrap(Session.class);

    for (PersistentCollectionChangeData changeData : collectionChanges) {
      log.debug("changeData entityName={}, data={}", changeData.getEntityName(), changeData.getData());
      @SuppressWarnings("unchecked") final Map<String, Object> data = changeData.getData();
      @SuppressWarnings("unchecked") final Map<String, Object> originalId =
          (Map<String, Object>) data.get(originalIdProp);

      originalId.put(revisionField, revisionData);

      if (AuditStrategyType.HORIZONTAL.equals(auditStrategyType)) {
        auditStrategy.performCollectionChange(
            sessionImplementor,
            getEntityName(),
            referencingPropertyName,
            enversService.getConfig(),
            changeData,
            revisionData
        );

        continue;
      }

      final Object revTypeValue = data.get(revTypeProp);
      final RevisionType revisionType = (revTypeValue instanceof RevisionType)
          ? (RevisionType) revTypeValue
          : RevisionType.valueOf(String.valueOf(revTypeValue));
      final boolean isDelete = RevisionType.DEL.equals(revisionType);

      for (Entry<String, Object> e : originalId.entrySet()) {
        final String key = e.getKey();
        if (revisionField.equalsIgnoreCase(key)) {
          continue;
        }

        final Object value = e.getValue();
        final String textValue = (value == null) ? null : String.valueOf(value);

        final AuditRecord record = auditRecords.computeIfAbsent(
            key,
            k -> new AuditRecord(changeData.getEntityName())
        );

        if (isDelete) {
          record.deleted = textValue;

        } else {
          record.added = textValue;
        }
      }
    }

    performVerticalAudit(auditRecords, sessionImplementor, revisionData);

    for (PersistentCollectionChangeData changeData : collectionChanges) {
      sessionCacheCleaner.scheduleAuditDataRemoval(session, changeData.getData());
    }
  }

  @Override
  public boolean containsWork() {
    return collectionChanges != null && !collectionChanges.isEmpty();
  }

  @Override
  public String getReferencingPropertyName() {
    return referencingPropertyName;
  }

  @Override
  public List<PersistentCollectionChangeData> getCollectionChanges() {
    return collectionChanges;
  }

  @Override
  public AuditWorkUnit dispatch(WorkUnitMergeVisitor first) {

    if (first instanceof PersistentCollectionChangeWorkUnit) {
      PersistentCollectionChangeWorkUnit original = (PersistentCollectionChangeWorkUnit) first;

      final Map<Object, PersistentCollectionChangeData> newChangesIdMap = new HashMap<>();
      for (PersistentCollectionChangeData persistentCollectionChangeData : getCollectionChanges()) {
        newChangesIdMap.put(
            getOriginalIdentifier(persistentCollectionChangeData),
            persistentCollectionChangeData);
      }

      final List<PersistentCollectionChangeData> mergedChanges = new ArrayList<>();

      for (PersistentCollectionChangeData originalCollectionChangeData : original.getCollectionChanges()) {
        final Object originalOriginalId = getOriginalIdentifier(originalCollectionChangeData);
        if (!newChangesIdMap.containsKey(originalOriginalId)) {
          mergedChanges.add(originalCollectionChangeData);

        } else {
          final String revTypePropName = enversService.getConfig().getRevisionTypePropertyName();
          if (RevisionType.ADD.equals(newChangesIdMap.get(originalOriginalId).getData().get(revTypePropName))
              && RevisionType.DEL.equals(originalCollectionChangeData.getData().get(revTypePropName))) {
            newChangesIdMap.remove(originalOriginalId);
          }
        }
      }

      mergedChanges.addAll(newChangesIdMap.values());

      return new NervPersistentCollectionChangeWorkUnit(
          sessionImplementor,
          entityName,
          enversService,
          id,
          mergedChanges,
          referencingPropertyName,
          auditStrategyType,
          auditFields);

    } else {
      throw new MergePersistentCollectionChangeWorkUnitException(first);
    }
  }

  @Override
  public Map<String, Object> generateData(Object revisionData) {
    throw new UnsupportedOperationException("Generate data is not supported for this work unit.");
  }

  private void performVerticalAudit(
      Map<String, AuditRecord> auditRecords,
      SharedSessionContractImplementor sessionImplementor,
      Object revisionData) {

    log.debug("auditRecords={}", auditRecords);
    for (Entry<String, AuditRecord> e : auditRecords.entrySet()) {
      final AuditRecord record = e.getValue();
      final String fieldName = e.getKey();

      if (record.deleted != null || record.added != null) {
        auditWorkUnit.perform(
            sessionImplementor,
            revisionData,
            null,
            fieldName.toUpperCase(Locale.ROOT),
            record.deleted,
            record.added,
            record.entityName
        );
      }
    }
  }

  private Object getOriginalIdentifier(PersistentCollectionChangeData persistentCollectionChangeData) {
    return persistentCollectionChangeData.getData()
        .get(enversService.getConfig().getOriginalIdPropertyName());
  }

  /**
   * Captures normalized audit metadata and serialized payload for one work unit record.
   */
  private static final class AuditRecord {

    private String added;
    private String deleted;
    private final String entityName;

    private AuditRecord(String entityName) {
      this.entityName = entityName;
    }
  }
}
