package com.czetsuyatech.audit.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.czetsuyatech.audit.infrastructure.envers.AuditConstant;
import com.czetsuyatech.audit.infrastructure.envers.AuditStrategyType;
import org.junit.jupiter.api.Test;

class AuditConfigTest {

  @Test
  void getAuditStrategyType_whenNull_returnsVertical() {

    AuditConfig config = AuditConfig.builder()
        .auditStrategyType(null)
        .build();
    assertThat(config.getAuditStrategyType())
        .isEqualTo(AuditStrategyType.VERTICAL);
  }

  @Test
  void getAuditStrategyType_whenVertical_returnsVertical() {

    AuditConfig config = AuditConfig.builder()
        .auditStrategyType(AuditStrategyType.VERTICAL)
        .build();
    assertThat(config.getAuditStrategyType())
        .isEqualTo(AuditStrategyType.VERTICAL);
  }

  @Test
  void getAuditStrategyType_whenHorizontal_returnsHorizontal() {

    AuditConfig config = AuditConfig.builder()
        .auditStrategyType(AuditStrategyType.HORIZONTAL)
        .build();
    assertThat(config.getAuditStrategyType())
        .isEqualTo(AuditStrategyType.HORIZONTAL);
  }

  @Test
  void isAuditInsert_whenNull_returnsFalse() {

    AuditConfig config = AuditConfig.builder()
        .auditInsert(null)
        .build();
    assertThat(config.isAuditInsert()).isFalse();
  }

  @Test
  void isAuditInsert_whenTrue_returnsTrue() {

    AuditConfig config = AuditConfig.builder()
        .auditInsert(Boolean.TRUE)
        .build();
    assertThat(config.isAuditInsert()).isTrue();
  }

  @Test
  void isAuditInsert_whenFalse_returnsFalse() {

    AuditConfig config = AuditConfig.builder()
        .auditInsert(Boolean.FALSE)
        .build();
    assertThat(config.isAuditInsert()).isFalse();
  }

  @Test
  void getAuditFields_whenNull_returnsDefaultFields() {

    AuditConfig config = AuditConfig.builder()
        .auditFields(null)
        .build();
    String[] defaults = AuditConstant.getAuditFields();
    assertThat(config.getAuditFields())
        .containsExactlyInAnyOrder(defaults);
  }

  @Test
  void getAuditFields_whenCustomFields_returnsParsedFields() {

    AuditConfig config = AuditConfig.builder()
        .auditFields("field1,field2,field3")
        .build();
    assertThat(config.getAuditFields())
        .containsExactlyInAnyOrder("field1", "field2", "field3");
  }

  @Test
  void getAuditFields_withSingleField_returnsSingleElementArray() {

    AuditConfig config = AuditConfig.builder()
        .auditFields("updatedBy")
        .build();
    assertThat(config.getAuditFields())
        .containsExactly("updatedBy");
  }

  @Test
  void defaultConstructor_hasNullFields() {

    AuditConfig config = new AuditConfig();

    assertThat(config.getAuditStrategyType())
        .isEqualTo(AuditStrategyType.VERTICAL);
    assertThat(config.isAuditInsert()).isFalse();
    assertThat(config.getAuditFields())
        .isEqualTo(AuditConstant.getAuditFields());
  }
}
