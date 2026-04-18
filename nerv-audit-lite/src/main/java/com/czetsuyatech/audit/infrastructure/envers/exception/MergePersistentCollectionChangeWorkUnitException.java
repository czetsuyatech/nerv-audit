package com.czetsuyatech.audit.infrastructure.envers.exception;

import org.hibernate.envers.internal.synchronization.work.WorkUnitMergeVisitor;

public class MergePersistentCollectionChangeWorkUnitException extends RuntimeException {

  public MergePersistentCollectionChangeWorkUnitException(WorkUnitMergeVisitor workUnitMergeVisitor) {
    super(String.format("Error merging %s", workUnitMergeVisitor));
  }
}
