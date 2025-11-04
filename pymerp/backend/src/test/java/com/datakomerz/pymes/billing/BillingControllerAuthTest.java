package com.datakomerz.pymes.billing;

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
 * Authorization tests for BillingController.
 * Validates RBAC rules for billing operations:
 * - GET: All roles
 * - POST operational: ADMIN, ERP_USER
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BillingControllerAuthTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @WithMockUser(roles = "READONLY")
  @DisplayName("POST /api/v1/billing/invoices - READONLY cannot emit invoice (403 Forbidden)")
  void testEmitInvoice_ReadonlyRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/billing/invoices")
        .header("X-Company-Id", UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"saleId\":\"" + UUID.randomUUID() + "\"}"))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "SETTINGS")
  @DisplayName("POST /api/v1/billing/invoices - SETTINGS cannot emit (403 Forbidden)")
  void testEmitInvoice_SettingsRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/billing/invoices")
        .header("X-Company-Id", UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"saleId\":\"" + UUID.randomUUID() + "\"}"))
      .andExpect(status().isForbidden());
  }
}
