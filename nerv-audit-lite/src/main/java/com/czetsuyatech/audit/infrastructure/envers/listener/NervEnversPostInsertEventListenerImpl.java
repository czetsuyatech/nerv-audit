package com.czetsuyatech.audit.infrastructure.envers.listener;

import com.czetsuyatech.audit.infrastructure.envers.AuditStrategyType;
import com.czetsuyatech.audit.infrastructure.envers.workunit.NervAddWorkUnit;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.event.spi.EnversPostInsertEventListenerImpl;
import org.hibernate.envers.internal.synchronization.AuditProcess;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.persister.entity.EntityPersister;

public class NervEnversPostInsertEventListenerImpl extends EnversPostInsertEventListenerImpl implements
    FieldNormalizer {

  private final AuditStrategyType auditStrategyType;
  private final boolean auditInsert;
  private final Set<String> auditFields;

  public NervEnversPostInsertEventListenerImpl(
      EnversService enversService,
      AuditStrategyType auditStrategyType,
      String[] auditFields,
      boolean auditInsert) {

    super(enversService);

    this.auditStrategyType = auditStrategyType;
    this.auditInsert = auditInsert;
    this.auditFields = normalizeAuditFields(auditFields);
  }

  @Override
  public void onPostInsert(PostInsertEvent event) {

    if (!auditInsert) {
      return;
    }

    final EntityPersister persister = event.getPersister();
    final String entityName = persister.getEntityName();

    if (!getEnversService().getEntitiesConfigurations().isVersioned(entityName)) {
      return;
    }

    checkIfTransactionInProgress(event.getSession());

    final AuditProcess auditProcess = getEnversService().getAuditProcessManager().get(event.getSession());
    final Map<String, Object> auditFieldValues = extractAuditFieldValues(event, persister);

    final NervAddWorkUnit workUnit = new NervAddWorkUnit(
        event.getSession(),
        entityName,
        getEnversService(),
        event.getId(),
        persister,
        event.getState(),
        auditStrategyType,
        auditFieldValues);

    auditProcess.addWorkUnit(workUnit);
  }

  private Map<String, Object> extractAuditFieldValues(PostInsertEvent event, EntityPersister persister) {
    final Map<String, Object> auditFieldValues = new HashMap<>();

    for (String field : auditFields) {
      auditFieldValues.put(field, null);
    }

    if (auditFields.isEmpty()) {
      return auditFieldValues;
    }

    final String[] propertyNames = persister.getPropertyNames();
    final Object[] state = event.getState();

    for (int i = 0; i < propertyNames.length; i++) {
      final String propertyUpper = propertyNames[i].toUpperCase(Locale.ROOT);
      if (auditFields.contains(propertyUpper)) {
        auditFieldValues.put(propertyUpper, state[i]);
      }
    }

    return auditFieldValues;
  }
}
