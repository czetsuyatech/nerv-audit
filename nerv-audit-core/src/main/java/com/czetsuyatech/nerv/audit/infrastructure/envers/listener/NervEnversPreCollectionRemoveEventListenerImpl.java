package com.czetsuyatech.nerv.audit.infrastructure.envers.listener;

import com.czetsuyatech.nerv.audit.infrastructure.envers.AuditStrategyType;
import java.io.Serializable;
import java.time.Clock;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.event.spi.PreCollectionRemoveEvent;
import org.hibernate.event.spi.PreCollectionRemoveEventListener;

@Slf4j
/**
 * Provides the NervEnversPreCollectionRemoveEventListenerImpl implementation.
 */
public class NervEnversPreCollectionRemoveEventListenerImpl
    extends NervBaseEnversCollectionEventListener
    implements PreCollectionRemoveEventListener {

  public NervEnversPreCollectionRemoveEventListenerImpl(
      EnversService enversService,
      AuditStrategyType auditStrategyType,
      String[] auditFields
  ) {
    this(enversService, auditStrategyType, auditFields, Clock.systemUTC());
  }

  public NervEnversPreCollectionRemoveEventListenerImpl(
      EnversService enversService,
      AuditStrategyType auditStrategyType,
      String[] auditFields,
      Clock clock
  ) {
    super(enversService, auditStrategyType, auditFields, clock);
  }

  @Override
  public void onPreRemoveCollection(PreCollectionRemoveEvent event) {

    log.debug("onPreRemoveCollection for={}, role={}", event.getCollection().getOwner(), event.getCollection().getRole());

    final CollectionEntry entry = getCollectionEntry(event);
    if (entry == null) {
      return;
    }

    if (entry.getLoadedPersister().isInverse()) {
      if (getEnversService().getConfig().isModifiedFlagsEnabled()) {
        initializeCollection(event);
      }
      return;
    }

    final var collection = event.getCollection();
    Serializable oldSnapshot = collection.getStoredSnapshot();
    if (oldSnapshot == null) {
      oldSnapshot = entry.getSnapshot();
    }

    if (!collection.wasInitialized() && shouldGenerateRevision(event)) {
      oldSnapshot = initializeCollection(event);
    }

    onCollectionAction(event, null, oldSnapshot, entry);
  }
}
