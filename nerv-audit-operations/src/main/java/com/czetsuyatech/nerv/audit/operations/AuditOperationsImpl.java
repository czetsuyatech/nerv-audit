package com.czetsuyatech.nerv.audit.operations;

import com.czetsuyatech.nerv.audit.application.dto.HorizontalAuditDTO;
import com.czetsuyatech.nerv.audit.application.dto.PageResult;
import com.czetsuyatech.nerv.audit.application.dto.VerticalAuditDTO;
import com.czetsuyatech.nerv.audit.application.query.AuditQuery;
import com.czetsuyatech.nerv.audit.service.AuditService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operations facade that keeps application-facing audit inspection separate from the core implementation.
 */
@RequiredArgsConstructor
public class AuditOperationsImpl implements AuditOperations {

  private final AuditService auditService;

  @Override
  @Transactional(readOnly = true)
  public PageResult<VerticalAuditDTO> search(AuditQuery query) {
    return auditService.getVerticalAudits(query);
  }

  @Override
  @Transactional(readOnly = true)
  public PageResult<VerticalAuditDTO> search(String entityName, AuditQuery query) {
    return auditService.getVerticalAudits(entityName, query);
  }

  @Override
  @Transactional(readOnly = true)
  public <T> List<HorizontalAuditDTO<T>> history(String entityName, AuditQuery query) {
    return auditService.getHorizontalAudits(entityName, query);
  }
}
