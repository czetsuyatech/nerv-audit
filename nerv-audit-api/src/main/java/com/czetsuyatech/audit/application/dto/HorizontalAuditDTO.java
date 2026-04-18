package com.czetsuyatech.audit.application.dto;

import java.time.Instant;
import lombok.Getter;

@Getter
public class HorizontalAuditDTO<T> {

  private final T entity;
  private final Number revision;
  private final Instant revisionDate;

  public HorizontalAuditDTO(T entity, Number revision, Instant revisionDate) {
    this.entity = entity;
    this.revision = revision;
    this.revisionDate = revisionDate;
  }
}
