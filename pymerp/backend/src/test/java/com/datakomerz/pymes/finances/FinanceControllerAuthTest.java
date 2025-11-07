package com.datakomerz.pymes.finances;

import static com.datakomerz.pymes.testsupport.AuthTestUtils.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Authorization tests for FinanceController.
 * Validates RBAC rules for financial operations:
 * - GET: ERP_USER, READONLY, ADMIN (all read-only endpoints)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.datakomerz.pymes.config.TestJwtDecoderConfig.class)
class FinanceControllerAuthTest {

  @Autowired
  private MockMvc mockMvc;
  private static final String COMPANY_ID = "00000000-0000-0000-0000-000000000001";

  @Test
  @DisplayName("GET /api/v1/finances/summary - READONLY can access (200 OK)")
  void testGetSummary_ReadonlyRole_Success() throws Exception {
    mockMvc.perform(get("/api/v1/finances/summary")
        .with(readonly())
        .header("X-Company-Id", COMPANY_ID))
      .andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /api/v1/finances/receivables - ERP_USER can access (200 OK)")
  void testGetReceivables_ErpUserRole_Success() throws Exception {
    mockMvc.perform(get("/api/v1/finances/receivables")
        .with(erpUser())
        .header("X-Company-Id", COMPANY_ID))
      .andExpect(status().isOk());
  }
}
