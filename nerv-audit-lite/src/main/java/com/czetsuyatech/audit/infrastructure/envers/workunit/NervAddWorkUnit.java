package com.czetsuyatech.audit.infrastructure.envers.workunit;

import com.czetsuyatech.audit.infrastructure.envers.AuditStrategyType;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.synchronization.SessionCacheCleaner;
import org.hibernate.envers.internal.synchronization.work.AuditWorkUnit;
import org.hibernate.envers.internal.synchronization.work.WorkUnitMergeVisitor;
import org.hibernate.envers.internal.tools.ArraysTools;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.util.StringUtils;

public class NervAddWorkUnit extends org.hibernate.envers.internal.synchronization.work.AddWorkUnit implements
    NervAuditPerformer {

  private final Object[] state;
  private final Map<String, Object> data;
  private final AuditStrategyType auditStrategyType;
  private final NervAuditWorkUnit auditWorkUnit;
  private final SessionCacheCleaner sessionCacheCleaner;
  private final Map<String, Object> auditFieldsValues;

  public NervAddWorkUnit(
      SharedSessionContractImplementor sessionImplementor,
      String entityName,
      EnversService enversService,
      Object id,
      EntityPersister entityPersister,
      Object[] state,
      AuditStrategyType auditStrategyType,
      Map<String, Object> auditFieldsValues
  ) {
    super(sessionImplementor, entityName, enversService, id, entityPersister, state);

    this.auditStrategyType = auditStrategyType;
    this.auditFieldsValues = auditFieldsValues;
    this.sessionCacheCleaner = new SessionCacheCleaner();
    this.data = new HashMap<>();
    this.state = state;
    this.enversService.getEntitiesConfigurations()
        .get(getEntityName())
        .getPropertyMapper()
        .map(sessionImplementor, data, entityPersister.getPropertyNames(), state, null);
    this.auditWorkUnit = new NervAuditWorkUnit(enversService, entityName, auditFieldsValues, getRevisionType());
  }

  public NervAddWorkUnit(
      SharedSessionContractImplementor sessionImplementor,
      String entityName,
      EnversService enversService,
      Object id,
      Map<String, Object> data,
      AuditStrategyType auditStrategyType,
      Map<String, Object> auditFieldsValues
  ) {
    super(sessionImplementor, entityName, enversService, id, data);

    this.auditStrategyType = auditStrategyType;
    this.auditFieldsValues = auditFieldsValues;
    this.sessionCacheCleaner = new SessionCacheCleaner();
    this.data = data;
    final String[] propertyNames = sessionImplementor.getFactory()
        .getRuntimeMetamodels()
        .getMappingMetamodel()
        .getEntityDescriptor(getEntityName())
        .getPropertyNames();
    this.state = ArraysTools.mapToArray(data, propertyNames);

    this.auditWorkUnit = new NervAuditWorkUnit(enversService, entityName, auditFieldsValues, getRevisionType());
  }

  @Override
  public AuditWorkUnit dispatch(WorkUnitMergeVisitor first) {

    if (first instanceof NervAddWorkUnit addWorkUnit) {
      return addWorkUnit.mergeCustom(this);
    }

    return first.merge(this);
  }

  @Override
  public void perform(SharedSessionContractImplementor sessionImplementor, Object revisionData) {

    delegatePerform(new NervAuditContext(
        auditStrategy,
        auditStrategyType,
        sessionImplementor,
        getEntityName(),
        enversService.getConfig(),
        id,
        revisionData,
        this::setPerformed));
  }

  @Override
  public void performVerticalAudit(SharedSessionContractImplementor sessionImplementor, Object revisionData,
      Map<String, Object> data) {
    final long entityId = Long.parseLong(String.valueOf(id));

    data.forEach((key, value) -> {
      if (value == null) {
        return;
      }
      if (!StringUtils.hasLength(String.valueOf(value))) {
        return;
      }
      if (auditFieldsValues.containsKey(key.toUpperCase())) {
        return;
      }

      auditWorkUnit.perform(
          sessionImplementor,
          revisionData,
          entityId,
          key,
          null,
          String.valueOf(value),
          null
      );
    });

    sessionCacheCleaner.scheduleAuditDataRemoval(sessionImplementor.unwrap(Session.class), data);
  }

  private static Map<String, Object> mergeModifiedFlagsCustom(
      Map<String, Object> lhs,
      Map<String, Object> rhs,
      Map<Integer, String> dirtyProperties
  ) {
    dirtyProperties.forEach((index, propertyName) -> {
      if (rhs.containsKey(propertyName)) {
        lhs.put(propertyName, rhs.get(propertyName));
      }
    });
    return lhs;
  }

  private AuditWorkUnit mergeCustom(NervAddWorkUnit second) {
    return second;
  }
}
