package com.czetsuyatech.nerv.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.czetsuyatech.nerv.audit.application.dto.PageResult;
import com.czetsuyatech.nerv.audit.application.query.AuditQuery;
import com.czetsuyatech.nerv.audit.persistence.SortDirection;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApiContractsTest {

  @Test
  void pageResultFactoryPreservesContentAndPagingDefaults() {
    PageResult<String> result = PageResult.of(List.of("first", "second"));

    assertEquals(List.of("first", "second"), result.getContent());
    assertEquals(0, result.getTotal());
    assertEquals(0, result.getOffset());
    assertEquals(50, result.getLimit());
  }

  @Test
  void auditQuerySetEntityReplacesAnyExistingEntityFilter() {
    AuditQuery query = AuditQuery.builder().entities(List.of("OldEntity")).build();

    query.setEntity("UserEntity");

    assertEquals(List.of("UserEntity"), query.getEntities());
  }

  @Test
  void sortDirectionUsesAscendingAsTheSafeDefault() {
    assertEquals(SortDirection.ASC, SortDirection.from(null));
    assertEquals(SortDirection.ASC, SortDirection.from("unexpected"));
    assertEquals(SortDirection.DESC, SortDirection.from("desc"));
  }

}
