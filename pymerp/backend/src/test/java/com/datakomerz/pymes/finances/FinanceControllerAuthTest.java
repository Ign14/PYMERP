package com.datakomerz.pymes.finances;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

/**
 * Authorization tests for FinanceController.
 * Validates RBAC rules for financial operations:
 * - GET: All roles
 * - POST: ADMIN, ERP_USER
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.datakomerz.pymes.config.TestJwtDecoderConfig.class)
class FinanceControllerAuthTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @WithMockUser(roles = "READONLY")
  @DisplayName("POST /api/v1/finances/payments - READONLY cannot record payment (403 Forbidden)")
  void testRecordPayment_ReadonlyRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/finances/payments")
        .header("X-Company-Id", UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"amount\":1000}"))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "SETTINGS")
  @DisplayName("POST /api/v1/finances/expenses - SETTINGS cannot record expense (403 Forbidden)")
  void testRecordExpense_SettingsRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/finances/expenses")
        .header("X-Company-Id", UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"amount\":500}"))
      .andExpect(status().isForbidden());
  }
}
