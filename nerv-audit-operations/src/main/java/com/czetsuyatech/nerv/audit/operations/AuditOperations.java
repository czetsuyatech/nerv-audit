package com.czetsuyatech.nerv.audit.operations;

import com.czetsuyatech.nerv.audit.application.dto.HorizontalAuditDTO;
import com.czetsuyatech.nerv.audit.application.dto.PageResult;
import com.czetsuyatech.nerv.audit.application.dto.VerticalAuditDTO;
import com.czetsuyatech.nerv.audit.application.query.AuditQuery;
import java.util.List;

/**
 * Transport-independent inspection API for audit records and entity history.
 */
public interface AuditOperations {

  PageResult<VerticalAuditDTO> search(AuditQuery query);

  PageResult<VerticalAuditDTO> search(String entityName, AuditQuery query);

  <T> List<HorizontalAuditDTO<T>> history(String entityName, AuditQuery query);
}
