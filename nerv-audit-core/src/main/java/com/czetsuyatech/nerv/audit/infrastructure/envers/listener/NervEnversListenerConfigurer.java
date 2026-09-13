package com.czetsuyatech.nerv.audit.infrastructure.envers.listener;

import com.czetsuyatech.nerv.audit.config.AuditConfig;
import jakarta.persistence.EntityManagerFactory;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.springframework.beans.factory.InitializingBean;

@RequiredArgsConstructor
@Slf4j
/**
 * Provides the NervEnversListenerConfigurer implementation.
 */
public class NervEnversListenerConfigurer implements InitializingBean {

  private final EntityManagerFactory entityManagerFactory;
  private final AuditConfig auditConfig;
  private final Clock clock;

  public NervEnversListenerConfigurer(EntityManagerFactory entityManagerFactory, AuditConfig auditConfig) {
    this(entityManagerFactory, auditConfig, Clock.systemUTC());
  }

  @Override
  public void afterPropertiesSet() {

    log.info("Initializing NERV | Audit");

    final ServiceRegistryImplementor serviceRegistry = entityManagerFactory
        .unwrap(SessionFactoryImplementor.class)
        .getServiceRegistry();
    final EnversService enversService = serviceRegistry.getService(EnversService.class);
    final EventListenerRegistry registry = serviceRegistry.getService(EventListenerRegistry.class);
    var auditStrategyType = auditConfig.getAuditStrategyType();
    var auditFields = auditConfig.getAuditFields();

    register(registry, EventType.POST_INSERT,
        new NervEnversPostInsertEventListenerImpl(
            enversService,
            auditStrategyType,
            auditFields,
            auditConfig.isAuditInsert(),
            clock));
    register(registry, EventType.POST_DELETE,
        new NervEnversPostDeleteEventListenerImpl(
            enversService,
            auditStrategyType,
            auditFields,
            clock));

    register(registry, EventType.POST_UPDATE,
        new NervEnversPostUpdateEventListenerImpl(
            enversService,
            auditStrategyType,
            auditFields,
            clock));
    register(registry, EventType.POST_COLLECTION_RECREATE,
        new NervEnversPostCollectionRecreateEventListenerImpl(
            enversService,
            auditStrategyType,
            auditFields,
            clock));
    register(registry, EventType.PRE_COLLECTION_REMOVE,
        new NervEnversPreCollectionRemoveEventListenerImpl(
            enversService,
            auditStrategyType,
            auditFields,
            clock));
    register(registry, EventType.PRE_COLLECTION_UPDATE,
        new NervEnversPreCollectionUpdateEventListenerImpl(
            enversService,
            auditStrategyType,
            auditFields,
            clock));

    log.info("NERV | Audit successfully started");
  }

  private static <T> void register(EventListenerRegistry registry, EventType<T> eventType, T listener) {
    registry.setListeners(eventType, listener);
  }
}
