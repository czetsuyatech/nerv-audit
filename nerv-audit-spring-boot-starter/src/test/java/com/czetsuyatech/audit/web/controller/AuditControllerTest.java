package com.czetsuyatech.audit.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.czetsuyatech.audit.application.dto.PageResult;
import com.czetsuyatech.audit.application.dto.VerticalAuditDTO;
import com.czetsuyatech.audit.infrastructure.license.LicenseService;
import com.czetsuyatech.audit.service.AuditService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuditController.class)
class AuditControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AuditService auditService;

  @MockitoBean
  private LicenseService licenseService;

  @Test
  void search_returnsOkWithEmptyResult() throws Exception {

    when(auditService.getVerticalAudits(any(), any()))
        .thenReturn(new PageResult<>(List.of(), 0, 0, 10));

    mockMvc.perform(get("/nerv-audit/vertical/UserEntity"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.total").value(0));
  }

  @Test
  void search_returnsAuditRecordsFromService() throws Exception {

    List<VerticalAuditDTO> dtos = List.of(
        VerticalAuditDTO.builder()
            .id(1L)
            .revisionNo(1L)
            .revisionType(1L)
            .updatedBy("admin")
            .updated(Instant.parse("2024-01-01T10:00:00Z"))
            .fieldName("FIRSTNAME")
            .oldValue("John")
            .newValue("Jane")
            .entityName("user_account_aud")
            .build()
    );
    when(auditService.getVerticalAudits(any(), any()))
        .thenReturn(new PageResult<>(dtos, 1, 0, 10));

    mockMvc.perform(get("/nerv-audit/vertical/UserEntity"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].fieldName").value("FIRSTNAME"))
        .andExpect(jsonPath("$.content[0].oldValue").value("John"))
        .andExpect(jsonPath("$.content[0].newValue").value("Jane"))
        .andExpect(jsonPath("$.content[0].updatedBy").value("admin"))
        .andExpect(jsonPath("$.total").value(1));
  }

  @Test
  void search_passesEntityNameToService() throws Exception {

    when(auditService.getVerticalAudits(any(), any()))
        .thenReturn(new PageResult<>(List.of(), 0, 0, 10));

    mockMvc.perform(get("/nerv-audit/vertical/SomeEntity"))
        .andExpect(status().isOk());

    verify(auditService).getVerticalAudits(eq("SomeEntity"), any());
  }

  @Test
  void search_withQueryParams_passesFiltersToService() throws Exception {

    when(auditService.getVerticalAudits(any(), any()))
        .thenReturn(new PageResult<>(List.of(), 0, 0, 10));

    mockMvc.perform(get("/nerv-audit/vertical/UserEntity")
            .param("updatedBy", "alice")
            .param("fieldName", "FIRSTNAME")
            .param("limit", "5")
            .param("offset", "0"))
        .andExpect(status().isOk());

    verify(auditService).getVerticalAudits(eq("UserEntity"), any());
  }

  @Test
  void search_withMultipleResults_returnsPaginatedResponse() throws Exception {

    List<VerticalAuditDTO> dtos = List.of(
        VerticalAuditDTO.builder()
            .id(1L)
            .fieldName("FIELD1")
            .build(),
        VerticalAuditDTO.builder()
            .id(2L)
            .fieldName("FIELD2")
            .build()
    );
    when(auditService.getVerticalAudits(any(), any()))
        .thenReturn(new PageResult<>(dtos, 100, 0, 2));

    mockMvc.perform(get("/nerv-audit/vertical/UserEntity")
            .param("limit", "2")
            .param("offset", "0"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.total").value(100))
        .andExpect(jsonPath("$.limit").value(2))
        .andExpect(jsonPath("$.offset").value(0));
  }
}
