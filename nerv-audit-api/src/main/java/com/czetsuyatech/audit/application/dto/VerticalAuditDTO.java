package com.czetsuyatech.audit.application.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class VerticalAuditDTO {

  private Long id;
  private Long revisionNo;
  private Long revisionType;
  private String updatedBy;
  private Instant updated;
  private String fieldName;
  private String oldValue;
  private String newValue;
  private String entityName;
}
