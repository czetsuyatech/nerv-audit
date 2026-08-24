package com.czetsuyatech.nerv.audit.infrastructure.envers.listener;

import com.czetsuyatech.nerv.audit.infrastructure.envers.AuditStrategyType;
import com.czetsuyatech.nerv.audit.infrastructure.envers.workunit.NervDelWorkUnit;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.event.spi.EnversPostDeleteEventListenerImpl;
import org.hibernate.envers.internal.synchronization.AuditProcess;
import org.hibernate.envers.internal.synchronization.work.AuditWorkUnit;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.persister.entity.EntityPersister;

@Slf4j
/**
 * Provides the NervEnversPostDeleteEventListenerImpl implementation.
 */
public class NervEnversPostDeleteEventListenerImpl extends EnversPostDeleteEventListenerImpl implements FieldNormalizer {

  private final AuditStrategyType auditStrategyType;
  private final Set<String> auditFields;

  public NervEnversPostDeleteEventListenerImpl(
      EnversService enversService,
      AuditStrategyType auditStrategyType,
      String[] auditFields) {

    super(enversService);

    this.auditStrategyType = auditStrategyType;
    this.auditFields = normalizeAuditFields(auditFields);
  }

  @Override
  public void onPostDelete(PostDeleteEvent event) {

    final var session = event.getSession();
    final String entityName = event.getPersister().getEntityName();

    log.debug("onPostDelete for={}, id={}", entityName, event.getId());

    if (!getEnversService().getEntitiesConfigurations().isVersioned(entityName)) {
      return;
    }

    checkIfTransactionInProgress(session);

    final Map<String, Object> auditFieldValues = buildInitialAuditFieldValues();
    populateAuditValues(event, auditFieldValues);

    final AuditProcess auditProcess = getEnversService().getAuditProcessManager().get(session);
    final AuditWorkUnit workUnit =
        new NervDelWorkUnit(
            (SessionImplementor) session,
            entityName,
            getEnversService(),
            event.getId(),
            event.getPersister(),
            event.getDeletedState(),
            this.auditStrategyType,
            auditFieldValues);

    auditProcess.addWorkUnit(workUnit);
  }

  private Map<String, Object> buildInitialAuditFieldValues() {

    if (auditFields.isEmpty()) {
      return new HashMap<>();
    }

    final Map<String, Object> auditFieldValues = new HashMap<>(Math.max(16, auditFields.size() * 2));
    for (String field : auditFields) {
      auditFieldValues.put(field, null);
    }

    return auditFieldValues;
  }

  private void populateAuditValues(PostDeleteEvent event, Map<String, Object> auditFieldValues) {

    if (auditFields.isEmpty()) {
      return;
    }

    final EntityPersister entityPersister = event.getPersister();
    final String[] propertyNames = entityPersister.getPropertyNames();
    final Object[] deletedState = event.getDeletedState();

    if (deletedState == null || propertyNames == null) {
      return;
    }

    final int len = Math.min(propertyNames.length, deletedState.length);
    for (int i = 0; i < len; i++) {
      final String prop = propertyNames[i];
      if (prop == null) {
        continue;
      }

      final String key = prop.toUpperCase(Locale.ROOT);
      if (auditFields.contains(key)) {
        auditFieldValues.put(key, deletedState[i]);
      }
    }
  }
}
