package com.czetsuyatech.nerv.audit.infrastructure.envers.listener;

import com.czetsuyatech.nerv.audit.infrastructure.envers.AuditStrategyType;
import com.czetsuyatech.nerv.audit.infrastructure.envers.workunit.NervPersistentCollectionChangeWorkUnit;
import java.io.Serializable;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.event.spi.BaseEnversEventListener;
import org.hibernate.envers.internal.entities.EntityConfiguration;
import org.hibernate.envers.internal.entities.RelationDescription;
import org.hibernate.envers.internal.entities.RelationType;
import org.hibernate.envers.internal.synchronization.AuditProcess;
import org.hibernate.event.spi.AbstractCollectionEvent;
import org.hibernate.persister.collection.AbstractCollectionPersister;

@Slf4j
/**
 * Provides the NervBaseEnversCollectionEventListener implementation.
 */
public abstract class NervBaseEnversCollectionEventListener extends BaseEnversEventListener {

  private final Clock clock;

  private final AuditStrategyType auditStrategyType;
  private final List<String> auditFields;

  protected NervBaseEnversCollectionEventListener() {
    super(null);
    this.clock = Clock.systemUTC();
    this.auditStrategyType = null;
    this.auditFields = List.of();
  }

  protected NervBaseEnversCollectionEventListener(
      EnversService enversService,
      AuditStrategyType auditStrategyType,
      String[] auditFields
  ) {
    this(enversService, auditStrategyType, auditFields, Clock.systemUTC());
  }

  protected NervBaseEnversCollectionEventListener(
      EnversService enversService,
      AuditStrategyType auditStrategyType,
      String[] auditFields,
      Clock clock
  ) {

    super(enversService);
    this.clock = Objects.requireNonNull(clock, "clock");
    this.auditStrategyType = auditStrategyType;
    this.auditFields = toUppercaseListOrEmpty(auditFields);
  }

  protected final CollectionEntry getCollectionEntry(AbstractCollectionEvent event) {

    return event.getSession()
        .getPersistenceContext()
        .getCollectionEntry(event.getCollection());
  }

  protected final void onCollectionAction(
      AbstractCollectionEvent event,
      PersistentCollection newColl,
      Serializable oldColl,
      CollectionEntry collectionEntry) {

    if (!shouldGenerateRevision(event)) {
      return;
    }

    checkIfTransactionInProgress(event.getSession());

    final AuditProcess auditProcess = getEnversService().getAuditProcessManager()
        .get(event.getSession());

    final String entityName = event.getAffectedOwnerEntityName();
    final String referencingPropertyName = referencingPropertyName(collectionEntry);

    log.debug("onCollectionAction: entityName={}, referencingPropertyName={}", entityName, referencingPropertyName);

    final RelationDescription rd = searchForRelationDescription(entityName, referencingPropertyName);
    if (rd != null && rd.getMappedByPropertyName() != null) {
      return;
    }

    final NervPersistentCollectionChangeWorkUnit workUnit =
        new NervPersistentCollectionChangeWorkUnit(
            event.getSession(),
            entityName,
            getEnversService(),
            newColl,
            collectionEntry,
            oldColl,
            event.getAffectedOwnerIdOrNull(),
            referencingPropertyName,
            auditStrategyType,
            auditFields,
            clock);

    log.debug("adding workUnit to auditProcess");
    auditProcess.addWorkUnit(workUnit);
  }

  protected final void onCollectionActionInversed(
      AbstractCollectionEvent event,
      PersistentCollection newColl,
      Serializable oldColl,
      CollectionEntry collectionEntry) {

    if (!shouldGenerateRevision(event)) {
      return;
    }

    final String entityName = event.getAffectedOwnerEntityName();
    final String referencingPropertyName = referencingPropertyName(collectionEntry);

    log.debug("onCollectionActionInversed: entityName={}, referencingPropertyName={}", entityName,
        referencingPropertyName);

    final RelationDescription rd = searchForRelationDescription(entityName, referencingPropertyName);
    if (rd != null && (rd.getRelationType().equals(RelationType.TO_MANY_NOT_OWNING) || rd.getRelationType()
        .equals(RelationType.TO_MANY_MIDDLE))) {
      onCollectionAction(event, newColl, oldColl, collectionEntry);
    }
  }

  protected Serializable initializeCollection(AbstractCollectionEvent event) {

    event.getCollection().forceInitialization();
    return event.getCollection().getStoredSnapshot();
  }

  protected boolean shouldGenerateRevision(AbstractCollectionEvent event) {

    final String entityName = event.getAffectedOwnerEntityName();
    return getEnversService().getEntitiesConfigurations().isVersioned(entityName);
  }

  private RelationDescription searchForRelationDescription(String entityName, String referencingPropertyName) {

    final EntityConfiguration configuration = getEnversService().getEntitiesConfigurations().get(entityName);
    final String propertyName = sanitizeReferencingPropertyName(referencingPropertyName);

    final RelationDescription rd = configuration.getRelationDescription(propertyName);
    if (rd != null) {
      return rd;
    }

    final String parentEntityName = configuration.getParentEntityName();
    if (parentEntityName == null) {
      return null;
    }

    return searchForRelationDescription(parentEntityName, propertyName);
  }

  private static String referencingPropertyName(CollectionEntry collectionEntry) {

    final String ownerEntityName = ((AbstractCollectionPersister) collectionEntry
        .getLoadedPersister())
        .getOwnerEntityName();

    return collectionEntry.getRole()
        .substring(ownerEntityName.length() + 1);
  }

  private static String sanitizeReferencingPropertyName(String propertyName) {

    if (propertyName == null || propertyName.indexOf('.') < 0) {
      return propertyName;
    }

    // replaceAll uses regex; replace is cheaper and clearer here
    return propertyName.replace('.', '_');
  }

  private static List<String> toUppercaseListOrEmpty(String[] fields) {

    if (fields == null || fields.length == 0) {
      return List.of();
    }

    return Arrays.stream(fields)
        .map(s -> s.toUpperCase(Locale.ROOT))
        .toList();
  }
}
