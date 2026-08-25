package com.czetsuyatech.nerv.audit.web.controller;

import com.czetsuyatech.nerv.audit.application.dto.HorizontalAuditDTO;
import com.czetsuyatech.nerv.audit.application.dto.PageResponse;
import com.czetsuyatech.nerv.audit.application.dto.PageResult;
import com.czetsuyatech.nerv.audit.application.dto.VerticalAuditDTO;
import com.czetsuyatech.nerv.audit.application.mapper.WebQuerySupport;
import com.czetsuyatech.nerv.audit.application.query.AuditQuery;
import com.czetsuyatech.nerv.audit.operations.AuditOperations;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP adapter over {@link AuditOperations}; it has no persistence implementation.
 */
@RestController
@RequestMapping("${nerv.audit.operations.web.base-path:/management/nerv-audit}/audits")
@RequiredArgsConstructor
public class AuditOperationsController {

  private final AuditOperations auditOperations;

  @GetMapping("/vertical")
  public PageResponse<VerticalAuditDTO> searchVerticalAudits(
      @RequestParam(required = false) Long id,
      @RequestParam(required = false) Long revisionNo,
      @RequestParam(required = false) String updatedBy,
      @RequestParam(required = false) String fieldName,
      @RequestParam(required = false) String newValue,
      @RequestParam(required = false) String oldValue,
      @RequestParam(required = false) String fromDate,
      @RequestParam(required = false) String toDate,
      @RequestParam(required = false) String sortBy,
      @RequestParam(required = false) String sortDirection,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    return toPage(auditOperations.search(query(
        id, revisionNo, updatedBy, fieldName, newValue, oldValue, fromDate, toDate, sortBy, sortDirection, page, size
    )));
  }

  @GetMapping("/vertical/{entity}")
  public PageResponse<VerticalAuditDTO> searchEntityVerticalAudits(
      @PathVariable String entity,
      @RequestParam(required = false) Long id,
      @RequestParam(required = false) Long revisionNo,
      @RequestParam(required = false) String updatedBy,
      @RequestParam(required = false) String fieldName,
      @RequestParam(required = false) String newValue,
      @RequestParam(required = false) String oldValue,
      @RequestParam(required = false) String fromDate,
      @RequestParam(required = false) String toDate,
      @RequestParam(required = false) String sortBy,
      @RequestParam(required = false) String sortDirection,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    return toPage(auditOperations.search(entity, query(
        id, revisionNo, updatedBy, fieldName, newValue, oldValue, fromDate, toDate, sortBy, sortDirection, page, size
    )));
  }

  @GetMapping("/horizontal/{entity}")
  public <T> List<HorizontalAuditDTO<T>> history(
      @PathVariable String entity,
      @RequestParam(defaultValue = "20") int size
  ) {
    return auditOperations.history(entity, AuditQuery.builder().limit(WebQuerySupport.pageSize(size)).build());
  }

  private static AuditQuery query(
      Long id,
      Long revisionNo,
      String updatedBy,
      String fieldName,
      String newValue,
      String oldValue,
      String fromDate,
      String toDate,
      String sortBy,
      String sortDirection,
      int page,
      int size
  ) {
    int pageNumber = WebQuerySupport.pageNumber(page);
    int pageSize = WebQuerySupport.pageSize(size);
    return AuditQuery.builder()
        .id(id)
        .revisionNo(revisionNo)
        .updatedBy(updatedBy)
        .fieldName(fieldName)
        .newValue(newValue)
        .oldValue(oldValue)
        .fromDate(WebQuerySupport.instant(fromDate, "fromDate"))
        .toDate(WebQuerySupport.instant(toDate, "toDate"))
        .sortBy(sortBy)
        .sortDirection(sortDirection)
        .offset(Math.multiplyExact(pageNumber, pageSize))
        .limit(pageSize)
        .build();
  }

  private static <T> PageResponse<T> toPage(PageResult<T> result) {
    int size = result.getLimit();
    long total = result.getTotal();
    return new PageResponse<>(
        result.getContent(),
        result.getOffset() / size,
        size,
        total,
        (int) Math.ceil((double) total / size)
    );
  }
}
