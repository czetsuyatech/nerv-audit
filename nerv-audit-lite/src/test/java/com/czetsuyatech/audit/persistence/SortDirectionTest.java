package com.czetsuyatech.audit.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SortDirectionTest {

  @Test
  void from_withDescUpperCase_returnsDesc() {

    assertThat(SortDirection.from("DESC"))
        .isEqualTo(SortDirection.DESC);
  }

  @Test
  void from_withDescLowerCase_returnsDesc() {

    assertThat(SortDirection.from("desc"))
        .isEqualTo(SortDirection.DESC);
  }

  @Test
  void from_withDescMixedCase_returnsDesc() {

    assertThat(SortDirection.from("Desc"))
        .isEqualTo(SortDirection.DESC);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"ASC", "asc", "Asc", "INVALID", "random"})
  void from_withNonDescValue_returnsAsc(String value) {

    assertThat(SortDirection.from(value))
        .isEqualTo(SortDirection.ASC);
  }

  @Test
  void enumValues_containsAscAndDesc() {

    assertThat(SortDirection.values())
        .containsExactlyInAnyOrder(SortDirection.ASC, SortDirection.DESC);
  }
}
