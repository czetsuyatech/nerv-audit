package com.czetsuyatech.nerv.audit.infrastructure.envers.listener;

import com.czetsuyatech.nerv.audit.infrastructure.envers.AuditStrategyType;
import java.time.Clock;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.event.spi.PreCollectionUpdateEvent;
import org.hibernate.event.spi.PreCollectionUpdateEventListener;

@Slf4j
/**
 * Provides the NervEnversPreCollectionUpdateEventListenerImpl implementation.
 */
public class NervEnversPreCollectionUpdateEventListenerImpl
    extends NervBaseEnversCollectionEventListener
    implements PreCollectionUpdateEventListener {

  public NervEnversPreCollectionUpdateEventListenerImpl(
      EnversService enversService,
      AuditStrategyType auditStrategyType,
      String[] auditFields) {
    this(enversService, auditStrategyType, auditFields, Clock.systemUTC());
  }

  public NervEnversPreCollectionUpdateEventListenerImpl(
      EnversService enversService,
      AuditStrategyType auditStrategyType,
      String[] auditFields,
      Clock clock
  ) {
    super(enversService, auditStrategyType, auditFields, clock);
  }

  @Override
  public void onPreUpdateCollection(PreCollectionUpdateEvent event) {

    log.debug("onPreUpdateCollection for={}, role={}", event.getCollection().getOwner(),
        event.getCollection().getRole());

    final CollectionEntry collectionEntry = getCollectionEntry(event);
    if (collectionEntry == null || collectionEntry.getLoadedPersister() == null) {
      return;
    }

    final var collection = event.getCollection();
    final var snapshot = collection.getStoredSnapshot();
    final boolean inverse = collectionEntry.getLoadedPersister().isInverse();

    if (inverse) {
      onCollectionActionInversed(event, collection, snapshot, collectionEntry);

    } else {
      onCollectionAction(event, collection, snapshot, collectionEntry);
    }
  }
}
