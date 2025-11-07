package com.datakomerz.pymes.sales.api;

import static com.datakomerz.pymes.testsupport.AuthTestUtils.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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

  private static final String COMPANY_ID = "00000000-0000-0000-0000-000000000001";

  @Test
  @DisplayName("GET /api/v1/reports/sales/summary - READONLY can access sales reports")
  void testGetSalesReport_ReadonlyRole_Success() throws Exception {
    mockMvc.perform(get("/api/v1/reports/sales/summary")
        .with(readonly())
        .header("X-Company-Id", COMPANY_ID)
        .param("days", "14"))
      .andExpect(status().isOk());
  }
}
