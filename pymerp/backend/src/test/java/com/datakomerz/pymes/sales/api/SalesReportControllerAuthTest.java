package com.datakomerz.pymes.sales.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

/**
 * Authorization tests for SalesReportController.
 * Validates RBAC rules for reporting:
 * - GET: All roles (reports are read-only)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SalesReportControllerAuthTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @WithMockUser(roles = "READONLY")
  @DisplayName("GET /api/v1/reports/sales - READONLY can access sales reports")
  void testGetSalesReport_ReadonlyRole_Success() throws Exception {
    mockMvc.perform(get("/api/v1/reports/sales")
        .header("X-Company-Id", UUID.randomUUID().toString())
        .param("startDate", "2025-01-01")
        .param("endDate", "2025-01-31"))
      .andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /api/v1/reports/sales - Anonymous gets 401 Unauthorized")
  void testGetSalesReport_Anonymous_Unauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/reports/sales")
        .param("startDate", "2025-01-01")
        .param("endDate", "2025-01-31"))
      .andExpect(status().isUnauthorized());
  }
}
