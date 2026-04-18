package com.czetsuyatech.audit.service;

import com.czetsuyatech.audit.application.dto.HorizontalAuditDTO;
import com.czetsuyatech.audit.application.dto.PageResult;
import com.czetsuyatech.audit.application.dto.VerticalAuditDTO;
import com.czetsuyatech.audit.application.query.AuditQuery;
import java.util.List;

public interface AuditService {

  PageResult<VerticalAuditDTO> getVerticalAudits(AuditQuery query);

  PageResult<VerticalAuditDTO> getVerticalAudits(String entityName, AuditQuery query);

  <T> List<HorizontalAuditDTO<T>> getHorizontalAudits(String entity, AuditQuery criteria);
}
