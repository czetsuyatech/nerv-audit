package com.czetsuyatech.audit.web.controller;

import com.czetsuyatech.audit.application.dto.HorizontalAuditDTO;
import com.czetsuyatech.audit.application.dto.PageResult;
import com.czetsuyatech.audit.application.dto.VerticalAuditDTO;
import com.czetsuyatech.audit.application.query.AuditQuery;
import com.czetsuyatech.audit.service.AuditService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/nerv-audit")
@RequiredArgsConstructor
public class AuditController {

  private final AuditService service;

  @GetMapping("/vertical/{entity}")
  public PageResult<VerticalAuditDTO> getVerticalAudits(@PathVariable String entity, AuditQuery criteria) {
    return service.getVerticalAudits(entity, criteria);
  }

  @GetMapping("/horizontal/{entity}")
  public <T> List<HorizontalAuditDTO<T>> getHorizontalAudits(@PathVariable String entity, AuditQuery criteria) {
    return service.getHorizontalAudits(entity, criteria);
  }
}
