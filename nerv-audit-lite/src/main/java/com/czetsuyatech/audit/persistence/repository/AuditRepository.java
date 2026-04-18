package com.czetsuyatech.audit.persistence.repository;

import com.czetsuyatech.audit.application.dto.PageResult;
import com.czetsuyatech.audit.application.dto.VerticalAuditDTO;
import com.czetsuyatech.audit.application.query.AuditQuery;
import com.czetsuyatech.audit.persistence.AuditSqlBuilder;
import com.czetsuyatech.audit.persistence.AuditTableResolver;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AuditRepository {

  private final EntityManager entityManager;
  private final AuditSqlBuilder queryBuilder;
  private final AuditTableResolver auditTableResolver;

  public PageResult<VerticalAuditDTO> findAuditsByQuery(AuditQuery q) {

    List<String> tables = new ArrayList<>();

    if (q.getEntities() != null) {
      tables.addAll(
          q.getEntities().stream()
              .map(auditTableResolver::resolve)
              .flatMap(opt -> opt.map(Stream::of)
                  .orElseGet(Stream::empty))
              .collect(Collectors.toList())
      );

    }

    Optional<String> unionSqlOpt = tables.stream()
        .map(t -> "SELECT id, rev, revtype, updated_by, updated, " +
            "field_name, old_value, new_value, '" + t + "' AS entity_name FROM " + t)
        .reduce((a, b) -> a + " UNION ALL " + b);

    if (unionSqlOpt.isEmpty()) {
      return new PageResult<>(List.of(), 0,
          Optional.ofNullable(q.getOffset()).orElse(0),
          Optional.ofNullable(q.getLimit()).orElse(10));
    }

    String unionSql = unionSqlOpt.get();

    Map<String, Object> params = new HashMap<>();
    String whereClause = queryBuilder.buildWhereClause(q, params);
    String baseSql = " FROM (" + unionSql + ") t " + whereClause;

    // Data query
    String dataSql = "SELECT * " + baseSql + queryBuilder.buildOrderBy(q);
    Query dataQuery = entityManager.createNativeQuery(dataSql);
    params.forEach(dataQuery::setParameter);
    Integer offset = Optional.ofNullable(q.getOffset())
        .orElse(0);
    dataQuery.setFirstResult(offset);
    Integer limit = Optional.ofNullable(q.getLimit())
        .orElse(10);
    dataQuery.setMaxResults(limit);
    List<Object[]> rows = dataQuery.getResultList();

    // Count query
    String countSql = "SELECT COUNT(*) " + baseSql;
    Query countQuery = entityManager.createNativeQuery(countSql);
    params.forEach(countQuery::setParameter);
    long total = ((Number) countQuery.getSingleResult()).longValue();

    return new PageResult<>(map(rows), total, offset, limit);
  }

  private List<VerticalAuditDTO> map(List<Object[]> rows) {

    return rows.stream().map(r -> new VerticalAuditDTO(
        ((Number) r[0]).longValue(),
        ((Number) r[1]).longValue(),
        ((Number) r[2]).longValue(),
        (String) r[3],
        ((LocalDateTime) r[4]).atZone(ZoneId.systemDefault()).toInstant(),
        (String) r[5],
        (String) r[6],
        (String) r[7],
        (String) r[8]
    )).toList();
  }
}
