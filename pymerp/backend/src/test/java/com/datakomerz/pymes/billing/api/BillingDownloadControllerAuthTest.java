package com.datakomerz.pymes.billing.api;

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
 * Authorization tests for BillingDownloadController.
 * Validates RBAC rules for invoice downloads:
 * - GET: All roles (downloads are read-only)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BillingDownloadControllerAuthTest {

  @Autowired
  private MockMvc mockMvc;

  private static final UUID INVOICE_ID = UUID.randomUUID();

  @Test
  @WithMockUser(roles = "READONLY")
  @DisplayName("GET /api/v1/billing/invoices/{id}/pdf - READONLY can download PDF")
  void testDownloadInvoicePdf_ReadonlyRole_Success() throws Exception {
    mockMvc.perform(get("/api/v1/billing/invoices/{id}/pdf", INVOICE_ID)
        .header("X-Company-Id", UUID.randomUUID().toString()))
      .andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /api/v1/billing/invoices/{id}/xml - Anonymous gets 401 Unauthorized")
  void testDownloadInvoiceXml_Anonymous_Unauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/billing/invoices/{id}/xml", INVOICE_ID))
      .andExpect(status().isUnauthorized());
  }
}
