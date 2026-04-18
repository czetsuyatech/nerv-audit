package com.czetsuyatech.audit.infrastructure.envers.listener;

import com.czetsuyatech.audit.config.AuditConfig;
import com.czetsuyatech.audit.infrastructure.license.LicenseService;
import jakarta.persistence.EntityManagerFactory;
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
public class NervEnversListenerConfigurer implements InitializingBean {

  private final EntityManagerFactory entityManagerFactory;
  private final AuditConfig auditConfig;
  private final LicenseService licenseService;

  @Override
  public void afterPropertiesSet() {

    final ServiceRegistryImplementor serviceRegistry =
        entityManagerFactory.unwrap(SessionFactoryImplementor.class).getServiceRegistry();
    final EnversService enversService = serviceRegistry.getService(EnversService.class);
    final EventListenerRegistry listenerRegistry = serviceRegistry.getService(EventListenerRegistry.class);

    var enversPostInsertEventListener =
        new NervEnversPostInsertEventListenerImpl(
            enversService,
            auditConfig.getAuditStrategyType(),
            auditConfig.getAuditFields(),
            auditConfig.isAuditInsert()
        );

    listenerRegistry.setListeners(EventType.POST_INSERT, enversPostInsertEventListener);

    log.info("NERV | Audit successfully starter");
  }
}
