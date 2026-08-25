package com.czetsuyatech.nerv.audit.operations.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.czetsuyatech.nerv.audit.application.dto.PageResult;
import com.czetsuyatech.nerv.audit.application.dto.VerticalAuditDTO;
import com.czetsuyatech.nerv.audit.operations.AuditOperations;
import com.czetsuyatech.nerv.audit.web.advice.AuditOperationsWebExceptionHandler;
import com.czetsuyatech.nerv.audit.web.controller.AuditOperationsController;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuditOperationsControllerTest {

  private final AuditOperations operations = org.mockito.Mockito.mock(AuditOperations.class);
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.standaloneSetup(new AuditOperationsController(operations))
        .setControllerAdvice(new AuditOperationsWebExceptionHandler())
        .addPlaceholderValue("nerv.audit.operations.web.base-path", "/management/nerv-audit")
        .build();
  }

  @Test
  void exposesPaginatedVerticalAuditsThroughTheOperationsBoundary() throws Exception {
    when(operations.search(eq("UserEntity"), any())).thenReturn(new PageResult<>(
        List.of(VerticalAuditDTO.builder()
            .id(1L)
            .fieldName("email")
            .updated(Instant.parse("2026-08-24T00:00:00Z"))
            .build()),
        1,
        20,
        20
    ));

    mvc.perform(get("/management/nerv-audit/audits/vertical/UserEntity")
            .param("page", "1")
            .param("size", "20")
            .param("updatedBy", "alice"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].fieldName").value("email"))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.totalElements").value(1));

    verify(operations).search(eq("UserEntity"), any());
  }

  @Test
  void rejectsInvalidPaginationAndDates() throws Exception {
    mvc.perform(get("/management/nerv-audit/audits/vertical").param("size", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("NERV_AUDIT_INVALID_QUERY"));
    mvc.perform(get("/management/nerv-audit/audits/vertical").param("fromDate", "invalid"))
        .andExpect(status().isBadRequest());
  }
}
