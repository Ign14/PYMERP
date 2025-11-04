package com.datakomerz.pymes.company;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

/**
 * Authorization tests for CompanyController.
 * Validates RBAC rules for company configuration:
 * - GET: All roles
 * - PUT: ADMIN, SETTINGS
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompanyControllerAuthTest {

  @Autowired
  private MockMvc mockMvc;

  private static final UUID COMPANY_ID = UUID.randomUUID();

  @Test
  @WithMockUser(roles = "ERP_USER")
  @DisplayName("PUT /api/v1/companies/{id} - ERP_USER cannot update (403 Forbidden)")
  void testUpdateCompany_ErpUserRole_Forbidden() throws Exception {
    mockMvc.perform(put("/api/v1/companies/{id}", COMPANY_ID)
        .header("X-Company-Id", COMPANY_ID.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"Updated Company\"}"))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "READONLY")
  @DisplayName("PUT /api/v1/companies/{id}/logo - READONLY cannot update logo (403 Forbidden)")
  void testUpdateCompanyLogo_ReadonlyRole_Forbidden() throws Exception {
    mockMvc.perform(put("/api/v1/companies/{id}/logo", COMPANY_ID)
        .header("X-Company-Id", COMPANY_ID.toString())
        .contentType(MediaType.MULTIPART_FORM_DATA))
      .andExpect(status().isForbidden());
  }
}
