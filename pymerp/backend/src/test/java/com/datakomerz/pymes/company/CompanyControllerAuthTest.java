package com.datakomerz.pymes.company;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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
@Import(com.datakomerz.pymes.config.TestJwtDecoderConfig.class)
class CompanyControllerAuthTest {

  @Autowired
  private MockMvc mockMvc;

  private static final UUID COMPANY_ID = UUID.randomUUID();
  private static final String COMPANY_PAYLOAD = """
      {
        "businessName": "Updated Company",
        "rut": "76000000-0",
        "businessActivity": "Servicios",
        "address": "Av. Principal 123",
        "commune": "Santiago",
        "phone": "+56 2 1234 5678",
        "email": "contacto@empresa.test",
        "receiptFooterMessage": "Gracias por su preferencia"
      }
      """;

  @Test
  @WithMockUser(roles = "ERP_USER")
  @DisplayName("PUT /api/v1/companies/{id} - ERP_USER cannot update (403 Forbidden)")
  void testUpdateCompany_ErpUserRole_Forbidden() throws Exception {
    mockMvc.perform(put("/api/v1/companies/{id}", COMPANY_ID)
        .header("X-Company-Id", COMPANY_ID.toString())
        .contentType("application/json")
        .content(COMPANY_PAYLOAD))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "READONLY")
  @DisplayName("POST /api/v1/companies - READONLY cannot create company (403 Forbidden)")
  void testCreateCompany_ReadonlyRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/companies")
        .header("X-Company-Id", UUID.randomUUID().toString())
        .contentType("application/json")
        .content(COMPANY_PAYLOAD))
      .andExpect(status().isForbidden());
  }
}
