package com.czetsuyatech.nerv.audit.service;

import com.czetsuyatech.nerv.audit.application.dto.HorizontalAuditDTO;
import com.czetsuyatech.nerv.audit.application.dto.PageResult;
import com.czetsuyatech.nerv.audit.application.dto.VerticalAuditDTO;
import com.czetsuyatech.nerv.audit.application.query.AuditQuery;
import java.util.List;

public interface AuditService {

  PageResult<VerticalAuditDTO> getVerticalAudits(AuditQuery query);

  PageResult<VerticalAuditDTO> getVerticalAudits(String entityName, AuditQuery query);

  <T> List<HorizontalAuditDTO<T>> getHorizontalAudits(String entity, AuditQuery criteria);
}
