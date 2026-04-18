package com.czetsuyatech.audit.application.query;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditQuery {

  private List<String> entities;

  // Filters
  private Long id;
  private Long revisionNo;
  private String updatedBy;
  private String fieldName;
  private String newValue;
  private String oldValue;
  private Instant fromDate;
  private Instant toDate;

  // Paging
  private Integer offset;
  private Integer limit;
  private String sortBy;
  private String sortDirection;

  public void setEntity(String entityName) {
    this.entities = List.of(entityName);
  }
}
