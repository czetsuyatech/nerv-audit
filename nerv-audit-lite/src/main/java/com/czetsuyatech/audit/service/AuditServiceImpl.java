package com.czetsuyatech.audit.service;

import com.czetsuyatech.audit.application.dto.HorizontalAuditDTO;
import com.czetsuyatech.audit.application.dto.PageResult;
import com.czetsuyatech.audit.application.dto.VerticalAuditDTO;
import com.czetsuyatech.audit.application.query.AuditQuery;
import com.czetsuyatech.audit.persistence.repository.AuditRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

  private final AuditRepository repository;
  private final EntityManager entityManager;

  /**
   * Convenience method to search a single entity by name.
   */
  @Override
  public PageResult<VerticalAuditDTO> getVerticalAudits(String entityName, AuditQuery query) {

    query.setEntity(entityName);

    return getVerticalAudits(query);
  }

  /**
   * Shortcut to search all entities defined in registry. If no entity is set in the query, repository.search will
   * search all tables
   */
  @Override
  public PageResult<VerticalAuditDTO> getVerticalAudits(AuditQuery query) {
    return repository.findAuditsByQuery(query);
  }

  @Override
  public <T> List<HorizontalAuditDTO<T>> getHorizontalAudits(String entityName, AuditQuery criteria) {

    int maxResults = Optional.ofNullable(criteria.getLimit()).orElse(10);

    Class<T> entityClass = (Class<T>) resolveEntity(entityName);

    AuditReader auditReader = AuditReaderFactory.get(entityManager);

    List<Object[]> results = auditReader.createQuery()
        .forRevisionsOfEntity(entityClass, false, true)
        .setMaxResults(maxResults)
        .addOrder(AuditEntity.revisionNumber().asc())
        .getResultList();

    return results.stream()
        .map(row -> {
          DefaultRevisionEntity revEntity = (DefaultRevisionEntity) row[1];
          return new HorizontalAuditDTO<>((T) row[0], revEntity.getId(), revEntity.getRevisionDate().toInstant());
        })
        .toList();
  }

  private Class<?> resolveEntity(String simpleName) {

    for (EntityType<?> entity : entityManager.getMetamodel().getEntities()) {
      Class<?> clazz = entity.getJavaType();

      if (clazz.getSimpleName().equals(simpleName)) {
        return clazz;
      }
    }

    throw new IllegalArgumentException("Entity not found: " + simpleName);
  }
}
