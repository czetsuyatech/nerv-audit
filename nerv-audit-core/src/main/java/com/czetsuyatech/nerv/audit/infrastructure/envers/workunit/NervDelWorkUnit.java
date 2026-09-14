package com.czetsuyatech.nerv.audit.infrastructure.envers.workunit;

import com.czetsuyatech.nerv.audit.infrastructure.envers.AuditStrategyType;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.synchronization.SessionCacheCleaner;
import org.hibernate.envers.internal.synchronization.work.AuditWorkUnit;
import org.hibernate.envers.internal.synchronization.work.DelWorkUnit;
import org.hibernate.envers.internal.synchronization.work.WorkUnitMergeVisitor;
import org.hibernate.envers.internal.tools.ArraysTools;
import org.hibernate.persister.entity.EntityPersister;

@Slf4j
/**
 * Provides the NervDelWorkUnit implementation.
 */
public class NervDelWorkUnit extends DelWorkUnit implements AuditWorkUnit, NervAuditPerformer {

  private static final String DELETED_SENTINEL = " ";

  private final Clock clock;

  private final Object[] state;
  private final EntityPersister entityPersister;
  private final String[] propertyNames;
  private final NervAuditWorkUnit auditWorkUnit;
  private final Map<String, Object> auditFieldsValues;
  private final AuditStrategyType auditStrategyType;
  private final SessionCacheCleaner sessionCacheCleaner = new SessionCacheCleaner();

  public NervDelWorkUnit(
      SessionImplementor sessionImplementor,
      String entityName,
      EnversService enversService,
      Object id,
      EntityPersister entityPersister,
      Object[] state,
      AuditStrategyType auditStrategyType,
      Map<String, Object> auditFieldsValues) {
    this(
        sessionImplementor,
        entityName,
        enversService,
        id,
        entityPersister,
        state,
        auditStrategyType,
        auditFieldsValues,
        Clock.systemUTC()
    );
  }

  public NervDelWorkUnit(
      SessionImplementor sessionImplementor,
      String entityName,
      EnversService enversService,
      Object id,
      EntityPersister entityPersister,
      Object[] state,
      AuditStrategyType auditStrategyType,
      Map<String, Object> auditFieldsValues,
      Clock clock
  ) {

    super(sessionImplementor, entityName, enversService, id, entityPersister, state);
    this.clock = Objects.requireNonNull(clock, "clock");

    log.debug("constructor for={}, id={}", entityName, id);

    this.auditFieldsValues = auditFieldsValues;
    this.auditStrategyType = auditStrategyType;
    this.state = state;
    this.entityPersister = entityPersister;
    this.propertyNames = entityPersister.getPropertyNames();
    this.auditWorkUnit =
        new NervAuditWorkUnit(enversService, entityName, auditFieldsValues, getRevisionType(), clock);
  }

  @Override
  public void perform(SharedSessionContractImplementor sessionImplementor, Object revisionData) {

    log.debug("perform called for={}", getEntityName());

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
  public void performVerticalAudit(
      SharedSessionContractImplementor sessionImplementor,
      Object revisionData,
      Map<String, Object> data) {

    auditWorkUnit.perform(sessionImplementor, revisionData, toLongId(id), DELETED_SENTINEL, null, null, null);
    sessionCacheCleaner.scheduleAuditDataRemoval(sessionImplementor.unwrap(Session.class), data);
  }

  @Override
  public AuditWorkUnit dispatch(WorkUnitMergeVisitor first) {

    if (first instanceof NervModWorkUnit mod) {
      return mod.mergeCustom(this);
    }

    if (first instanceof NervAddWorkUnit) {
      return null;
    }

    if (first instanceof NervDelWorkUnit del) {
      return del;
    }

    return first.merge(this);
  }

  @Override
  public boolean containsWork() {
    return true;
  }

  @Override
  public Map<String, Object> generateData(Object revisionData) {

    final Map<String, Object> data = new HashMap<>(propertyNames.length + 8);
    fillDataWithId(data, revisionData);

    enversService
        .getEntitiesConfigurations()
        .get(getEntityName())
        .getPropertyMapper()
        .map(sessionImplementor, data, propertyNames, null, state);

    return data;
  }

  public AuditWorkUnit mergeCustom(NervAddWorkUnit second) {

    if (ArraysTools.arraysEqual(second.getState(), state)) {
      return null;
    }

    final Object[] secondState = second.getState();
    final Map<Integer, String> dirtyProp = new HashMap<>();

    for (int i = 0; i < propertyNames.length; i++) {
      if (!Objects.equals(secondState[i], state[i])) {
        dirtyProp.put(i, propertyNames[i]);
      }
    }

    return new NervModWorkUnit(
        sessionImplementor,
        entityName,
        enversService,
        id,
        entityPersister,
        secondState,
        state,
        dirtyProp,
        auditStrategyType,
        auditFieldsValues,
        clock);
  }

  private static long toLongId(Object id) {

    if (id == null) {
      throw new IllegalArgumentException("Audit id must not be null");
    }

    if (id instanceof Number n) {
      return n.longValue();
    }

    return Long.parseLong(String.valueOf(id));
  }
}
