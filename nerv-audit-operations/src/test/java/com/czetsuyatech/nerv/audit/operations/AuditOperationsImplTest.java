package com.czetsuyatech.nerv.audit.operations;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.czetsuyatech.nerv.audit.application.dto.PageResult;
import com.czetsuyatech.nerv.audit.application.query.AuditQuery;
import com.czetsuyatech.nerv.audit.service.AuditService;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuditOperationsImplTest {

  private final AuditService auditService = mock(AuditService.class);
  private final AuditOperations operations = new AuditOperationsImpl(auditService);

  @Test
  void delegatesReadOnlyInspectionToTheEstablishedAuditService() {
    AuditQuery query = AuditQuery.builder().limit(20).build();
    when(auditService.getVerticalAudits("UserEntity", query)).thenReturn(new PageResult<>(List.of()));

    operations.search("UserEntity", query);
    operations.history("UserEntity", query);
    operations.search(query);

    verify(auditService).getVerticalAudits("UserEntity", query);
    verify(auditService).getHorizontalAudits("UserEntity", query);
    verify(auditService).getVerticalAudits(query);
  }
}
