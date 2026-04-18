package com.czetsuyatech.audit.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.czetsuyatech.audit.application.query.AuditQuery;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AuditSqlBuilderTest {

  private AuditSqlBuilder builder;

  @BeforeEach
  void setUp() {
    builder = new AuditSqlBuilder();
  }

  // ---- buildWhereClause ----

  @Test
  void buildWhereClause_withEmptyQuery_returnsBaseWhereOnly() {

    AuditQuery q = AuditQuery.builder().build();
    Map<String, Object> params = new HashMap<>();

    String result = builder.buildWhereClause(q, params);

    assertThat(result)
        .containsIgnoringCase("WHERE 1=1");
    assertThat(params).isEmpty();
  }

  @Test
  void buildWhereClause_withId_addsIdFilter() {

    AuditQuery q = AuditQuery.builder()
        .id(42L)
        .build();
    Map<String, Object> params = new HashMap<>();

    String result = builder.buildWhereClause(q, params);

    assertThat(result)
        .contains("id = :id");
    assertThat(params)
        .containsEntry("id", 42L);
  }

  @Test
  void buildWhereClause_withRevisionNo_addsRevFilter() {

    AuditQuery q = AuditQuery.builder()
        .revisionNo(7L)
        .build();
    Map<String, Object> params = new HashMap<>();

    String result = builder.buildWhereClause(q, params);

    assertThat(result)
        .contains("rev = :revisionNo");
    assertThat(params)
        .containsEntry("revisionNo", 7L);
  }

  @Test
  void buildWhereClause_withUpdatedBy_addsUpdatedByFilter() {

    AuditQuery q = AuditQuery.builder()
        .updatedBy("alice")
        .build();
    Map<String, Object> params = new HashMap<>();

    String result = builder.buildWhereClause(q, params);

    assertThat(result)
        .contains("updated_by = :updatedBy");
    assertThat(params)
        .containsEntry("updatedBy", "alice");
  }

  @Test
  void buildWhereClause_withFieldName_addsFieldNameFilter() {

    AuditQuery q = AuditQuery.builder()
        .fieldName("firstName")
        .build();
    Map<String, Object> params = new HashMap<>();

    String result = builder.buildWhereClause(q, params);

    assertThat(result)
        .contains("field_name = :fieldName");
    assertThat(params)
        .containsEntry("fieldName", "firstName");
  }

  @Test
  void buildWhereClause_withNewValue_addsNewValueFilter() {

    AuditQuery q = AuditQuery.builder()
        .newValue("John")
        .build();
    Map<String, Object> params = new HashMap<>();

    String result = builder.buildWhereClause(q, params);

    assertThat(result)
        .contains("new_value = :newValue");
    assertThat(params)
        .containsEntry("newValue", "John");
  }

  @Test
  void buildWhereClause_withOldValue_addsOldValueFilter() {

    AuditQuery q = AuditQuery.builder()
        .oldValue("Jane")
        .build();
    Map<String, Object> params = new HashMap<>();

    String result = builder.buildWhereClause(q, params);

    assertThat(result)
        .contains("old_value = :oldValue");
    assertThat(params)
        .containsEntry("oldValue", "Jane");
  }

  @Test
  void buildWhereClause_withFromDate_addsFromDateFilter() {

    Instant from = Instant.parse("2024-01-01T00:00:00Z");
    AuditQuery q = AuditQuery.builder()
        .fromDate(from)
        .build();
    Map<String, Object> params = new HashMap<>();

    String result = builder.buildWhereClause(q, params);

    assertThat(result)
        .contains("updated >= :fromDate");
    assertThat(params)
        .containsEntry("fromDate", from);
  }

  @Test
  void buildWhereClause_withToDate_addsToDateFilter() {

    Instant to = Instant.parse("2024-12-31T23:59:59Z");
    AuditQuery q = AuditQuery.builder()
        .toDate(to)
        .build();
    Map<String, Object> params = new HashMap<>();

    String result = builder.buildWhereClause(q, params);

    assertThat(result)
        .contains("updated <= :toDate");
    assertThat(params)
        .containsEntry("toDate", to);
  }

  @Test
  void buildWhereClause_withAllFilters_buildsCompleteClause() {

    Instant from = Instant.parse("2024-01-01T00:00:00Z");
    Instant to = Instant.parse("2024-12-31T23:59:59Z");
    AuditQuery q = AuditQuery.builder()
        .id(1L)
        .revisionNo(2L)
        .updatedBy("bob")
        .fieldName("lastName")
        .newValue("Smith")
        .oldValue("Jones")
        .fromDate(from)
        .toDate(to)
        .build();
    Map<String, Object> params = new HashMap<>();

    String result = builder.buildWhereClause(q, params);

    assertThat(result)
        .contains("id = :id")
        .contains("rev = :revisionNo")
        .contains("updated_by = :updatedBy")
        .contains("field_name = :fieldName")
        .contains("new_value = :newValue")
        .contains("old_value = :oldValue")
        .contains("updated >= :fromDate")
        .contains("updated <= :toDate");
    assertThat(params)
        .hasSize(8);
  }

  // ---- buildOrderBy ----

  @Test
  void buildOrderBy_withNullSortBy_usesDefaultField() {

    AuditQuery q = AuditQuery.builder().build();

    String result = builder.buildOrderBy(q);

    assertThat(result)
        .containsIgnoringCase("ORDER BY updated");
  }

  @Test
  void buildOrderBy_withInvalidSortBy_usesDefaultField() {

    AuditQuery q = AuditQuery.builder()
        .sortBy("drop_table")
        .build();

    String result = builder.buildOrderBy(q);

    assertThat(result)
        .containsIgnoringCase("ORDER BY updated");
  }

  @ParameterizedTest
  @ValueSource(strings = {"id", "field_name", "rev", "updated", "updated_by"})
  void buildOrderBy_withValidSortFields_usesRequestedField(String field) {

    AuditQuery q = AuditQuery.builder()
        .sortBy(field)
        .build();

    String result = builder.buildOrderBy(q);

    assertThat(result)
        .containsIgnoringCase("ORDER BY " + field);
  }

  @Test
  void buildOrderBy_withNullDirection_defaultsToAsc() {

    AuditQuery q = AuditQuery.builder().build();

    String result = builder.buildOrderBy(q);

    assertThat(result)
        .containsIgnoringCase("ASC");
  }

  @Test
  void buildOrderBy_withDescDirection_returnsDesc() {

    AuditQuery q = AuditQuery.builder()
        .sortDirection("DESC")
        .build();

    String result = builder.buildOrderBy(q);

    assertThat(result)
        .containsIgnoringCase("DESC");
  }

  @Test
  void buildOrderBy_withAscDirection_returnsAsc() {

    AuditQuery q = AuditQuery.builder()
        .sortDirection("ASC")
        .build();

    String result = builder.buildOrderBy(q);

    assertThat(result)
        .containsIgnoringCase("ASC");
  }

  @Test
  void buildOrderBy_withUnknownDirection_defaultsToAsc() {

    AuditQuery q = AuditQuery.builder()
        .sortDirection("SIDEWAYS")
        .build();

    String result = builder.buildOrderBy(q);

    assertThat(result)
        .containsIgnoringCase("ASC");
  }
}
