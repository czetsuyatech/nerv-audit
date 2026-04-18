package com.czetsuyatech.audit.infrastructure.envers.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;

class FieldNormalizerTest {

  /** Minimal concrete implementation for testing the interface default method. */
  private final FieldNormalizer normalizer = new FieldNormalizer() {};

  @Test
  void normalizeAuditFields_withNullInput_returnsEmptySet() {

    Set<String> result = normalizer.normalizeAuditFields(null);
    assertThat(result).isEmpty();
  }

  @Test
  void normalizeAuditFields_withEmptyArray_returnsEmptySet() {

    Set<String> result = normalizer.normalizeAuditFields(new String[]{});
    assertThat(result).isEmpty();
  }

  @Test
  void normalizeAuditFields_withBlankStrings_filtersThemOut() {

    Set<String> result = normalizer.normalizeAuditFields(new String[]{"  ", "", "validField"});
    assertThat(result)
        .containsExactly("VALIDFIELD");
  }

  @Test
  void normalizeAuditFields_withLowerCaseFields_convertsToUpperCase() {

    Set<String> result = normalizer.normalizeAuditFields(new String[]{"updatedby", "updated"});
    assertThat(result)
        .containsExactlyInAnyOrder("UPDATEDBY", "UPDATED");
  }

  @Test
  void normalizeAuditFields_withMixedCaseFields_convertsToUpperCase() {

    Set<String> result = normalizer.normalizeAuditFields(new String[]{"updatedBy", "CreatedBy", "UPDATED"});
    assertThat(result)
        .containsExactlyInAnyOrder("UPDATEDBY", "CREATEDBY", "UPDATED");
  }

  @Test
  void normalizeAuditFields_withNullElementInArray_filtersNullOut() {

    Set<String> result = normalizer.normalizeAuditFields(new String[]{"field1", null, "field2"});
    assertThat(result)
        .containsExactlyInAnyOrder("FIELD1", "FIELD2");
  }

  @Test
  void normalizeAuditFields_resultIsUnmodifiable() {

    Set<String> result = normalizer.normalizeAuditFields(new String[]{"field1"});
    assertThatThrownBy(() -> result.add("newField"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void normalizeAuditFields_withDuplicates_returnsDistinctSet() {

    Set<String> result = normalizer.normalizeAuditFields(new String[]{"field", "FIELD", "Field"});
    assertThat(result)
        .hasSize(1)
        .containsExactly("FIELD");
  }

  @Test
  void normalizeAuditFields_withDefaultAuditFields_normalizesAll() {

    String[] fields = {"createdBy", "created", "updatedBy", "updated", "originalId", "revisionType", "version"};
    Set<String> result = normalizer.normalizeAuditFields(fields);
    assertThat(result)
        .hasSize(7)
        .allSatisfy(f -> assertThat(f).isUpperCase());
  }
}
