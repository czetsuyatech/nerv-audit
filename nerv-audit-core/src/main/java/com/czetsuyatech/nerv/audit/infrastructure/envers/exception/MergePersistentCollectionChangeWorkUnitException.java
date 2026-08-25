package com.czetsuyatech.nerv.audit.infrastructure.envers.exception;

import org.hibernate.envers.internal.synchronization.work.WorkUnitMergeVisitor;

/**
 * Provides the MergePersistentCollectionChangeWorkUnitException implementation.
 */
public class MergePersistentCollectionChangeWorkUnitException extends RuntimeException {

  public MergePersistentCollectionChangeWorkUnitException(WorkUnitMergeVisitor workUnitMergeVisitor) {
    super(String.format("Error merging %s", workUnitMergeVisitor));
  }
}
