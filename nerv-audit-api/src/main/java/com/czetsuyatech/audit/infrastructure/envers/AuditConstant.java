package com.czetsuyatech.audit.infrastructure.envers;

public final class AuditConstant {

  private AuditConstant() {
  }

  public static final String AUDIT_CREATED_BY = "createdBy";
  public static final String AUDIT_CREATED = "created";
  public static final String AUDIT_UPDATED_BY = "updatedBy";
  public static final String AUDIT_UPDATED = "updated";

  public static final String AUDIT_FIELD_ORIGINAL_ID = "originalId";
  public static final String AUDIT_FIELD_REVISION_TYPE = "revisionType";
  public static final String AUDIT_FIELD_VERSION = "version";

  public static String[] getAuditFields() {

    return new String[]{
        AUDIT_CREATED_BY,
        AUDIT_CREATED,
        AUDIT_UPDATED_BY,
        AUDIT_UPDATED,
        AUDIT_FIELD_ORIGINAL_ID,
        AUDIT_FIELD_REVISION_TYPE,
        AUDIT_FIELD_VERSION
    };
  }
}
