package com.czetsuyatech.audit.persistence;

import com.czetsuyatech.audit.application.query.AuditQuery;
import java.util.Map;
import java.util.Set;

public class AuditSqlBuilder {

  private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
      "id", "field_name", "rev", "updated", "updated_by"
  );

  private static final String DEFAULT_SORT_FIELD = "updated";

  public String buildWhereClause(AuditQuery q, Map<String, Object> params) {
    StringBuilder sql = new StringBuilder(" WHERE 1=1 ");

    if (q.getId() != null) {
      sql.append(" AND id = :id");
      params.put("id", q.getId());
    }

    if (q.getRevisionNo() != null) {
      sql.append(" AND rev = :revisionNo");
      params.put("revisionNo", q.getRevisionNo());
    }

    if (q.getUpdatedBy() != null) {
      sql.append(" AND updated_by = :updatedBy");
      params.put("updatedBy", q.getUpdatedBy());
    }

    if (q.getFieldName() != null) {
      sql.append(" AND field_name = :fieldName");
      params.put("fieldName", q.getFieldName());
    }

    if (q.getNewValue() != null) {
      sql.append(" AND new_value = :newValue");
      params.put("newValue", q.getNewValue());
    }

    if (q.getOldValue() != null) {
      sql.append(" AND old_value = :oldValue");
      params.put("oldValue", q.getOldValue());
    }

    if (q.getFromDate() != null) {
      sql.append(" AND updated >= :fromDate");
      params.put("fromDate", q.getFromDate());
    }

    if (q.getToDate() != null) {
      sql.append(" AND updated <= :toDate");
      params.put("toDate", q.getToDate());
    }

    return sql.toString();
  }

  public String buildOrderBy(AuditQuery q) {

    String field = resolveSortField(q.getSortBy());
    SortDirection direction = resolveDirection(q.getSortDirection());

    return " ORDER BY " + field + " " + direction;
  }

  private String resolveSortField(String sortBy) {

    if (sortBy == null) {
      return DEFAULT_SORT_FIELD;
    }

    if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
      return DEFAULT_SORT_FIELD;
    }

    return sortBy;
  }

  private SortDirection resolveDirection(String dir) {
    return SortDirection.from(dir);
  }
}
