package com.czetsuyatech.nerv.audit.infrastructure.envers.listener;

import com.czetsuyatech.nerv.audit.infrastructure.envers.AuditStrategyType;
import java.time.Clock;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.event.spi.PostCollectionRecreateEvent;
import org.hibernate.event.spi.PostCollectionRecreateEventListener;

@Slf4j
/**
 * Provides the NervEnversPostCollectionRecreateEventListenerImpl implementation.
 */
public class NervEnversPostCollectionRecreateEventListenerImpl
    extends NervBaseEnversCollectionEventListener
    implements PostCollectionRecreateEventListener {

  public NervEnversPostCollectionRecreateEventListenerImpl(
      EnversService enversService,
      AuditStrategyType auditStrategyType,
      String[] auditFields
  ) {
    this(enversService, auditStrategyType, auditFields, Clock.systemUTC());
  }

  public NervEnversPostCollectionRecreateEventListenerImpl(
      EnversService enversService,
      AuditStrategyType auditStrategyType,
      String[] auditFields,
      Clock clock
  ) {
    super(enversService, auditStrategyType, auditFields, clock);
  }

  @Override
  public void onPostRecreateCollection(PostCollectionRecreateEvent event) {

    log.debug("onPostRecreateCollection for={}, role={}", event.getCollection().getOwner(), event.getCollection().getRole());

    final CollectionEntry collectionEntry =
        Objects.requireNonNull(getCollectionEntry(event), "collectionEntry must not be null");

    final boolean inverse =
        Objects.requireNonNull(collectionEntry.getLoadedPersister(), "loadedPersister must not be null")
            .isInverse();

    final var collection = event.getCollection();
    if (inverse) {
      onCollectionActionInversed(event, collection, null, collectionEntry);
      return;
    }

    onCollectionAction(event, collection, null, collectionEntry);
  }
}
