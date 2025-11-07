package com.datakomerz.pymes.billing;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
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
@Import(com.datakomerz.pymes.config.TestJwtDecoderConfig.class)
class BillingControllerAuthTest {

  @Autowired
  private MockMvc mockMvc;

  private static final String VALID_INVOICE_PAYLOAD = """
      {
        "documentType": "FACTURA",
        "taxMode": "AFECTA",
        "idempotencyKey": "test-key",
        "sale": {
          "id": "00000000-0000-0000-0000-000000000001",
          "items": [
            {
              "productId": "00000000-0000-0000-0000-000000000010",
              "description": "Test item",
              "quantity": 1,
              "unitPrice": 1000,
              "discount": 0
            }
          ],
          "net": 1000,
          "vat": 190,
          "total": 1190,
          "customerName": "Test Customer",
          "customerTaxId": "12345678-9",
          "pointOfSale": "WEB",
          "deviceId": "POS-1"
        }
      }
      """;

  @Test
  @WithMockUser(roles = "READONLY")
  @DisplayName("POST /api/v1/billing/invoices - READONLY cannot emit invoice (403 Forbidden)")
  void testEmitInvoice_ReadonlyRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/billing/invoices")
        .header("X-Company-Id", UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content(VALID_INVOICE_PAYLOAD))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "SETTINGS")
  @DisplayName("POST /api/v1/billing/invoices - SETTINGS cannot emit (403 Forbidden)")
  void testEmitInvoice_SettingsRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/billing/invoices")
        .header("X-Company-Id", UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content(VALID_INVOICE_PAYLOAD))
      .andExpect(status().isForbidden());
  }
}
