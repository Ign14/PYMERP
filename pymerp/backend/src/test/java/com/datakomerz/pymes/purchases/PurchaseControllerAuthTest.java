package com.datakomerz.pymes.purchases;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.datakomerz.pymes.testsupport.AuthTestUtils.*;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Authorization tests for PurchaseController.
 * Validates RBAC rules for operational endpoints:
 * - GET: All roles (ADMIN, SETTINGS, ERP_USER, READONLY)
 * - POST/PUT operational: ADMIN, ERP_USER
 * - DELETE: ADMIN only
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PurchaseControllerAuthTest {

  @Autowired
  private MockMvc mockMvc;

  private static final UUID PURCHASE_ID = UUID.randomUUID();
  private static final String COMPANY_ID = "00000000-0000-0000-0000-000000000001";

  @Test
  @DisplayName("POST /api/v1/purchases - READONLY cannot create purchase (403 Forbidden)")
  void testCreatePurchase_ReadonlyRole_Forbidden() throws Exception {
    String body = "{" +
        "\"supplierId\":\"" + UUID.randomUUID() + "\"," +
        "\"docType\":\"BOLETA\"," +
        "\"net\":100.0," +
        "\"vat\":19.0," +
        "\"total\":119.0," +
        "\"issuedAt\":\"" + OffsetDateTime.now().toString() + "\"," +
        "\"items\":[{" +
        "\"productId\":\"" + UUID.randomUUID() + "\"," +
        "\"qty\":1.0," +
        "\"unitCost\":100.0" +
        "}]" +
        "}";

    mockMvc.perform(post("/api/v1/purchases")
        .with(readonly())
        .header("X-Company-Id", COMPANY_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
      .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("POST /api/v1/purchases - SETTINGS cannot create purchase (403 Forbidden)")
  void testCreatePurchase_SettingsRole_Forbidden() throws Exception {
    String body = "{" +
        "\"supplierId\":\"" + UUID.randomUUID() + "\"," +
        "\"docType\":\"BOLETA\"," +
        "\"net\":100.0," +
        "\"vat\":19.0," +
        "\"total\":119.0," +
        "\"issuedAt\":\"" + OffsetDateTime.now().toString() + "\"," +
        "\"items\":[{" +
        "\"productId\":\"" + UUID.randomUUID() + "\"," +
        "\"qty\":1.0," +
        "\"unitCost\":100.0" +
        "}]" +
        "}";

    mockMvc.perform(post("/api/v1/purchases")
        .with(settings())
        .header("X-Company-Id", COMPANY_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
      .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("PUT /api/v1/purchases/{id} - READONLY cannot update (403 Forbidden)")
  void testUpdatePurchase_ReadonlyRole_Forbidden() throws Exception {
    mockMvc.perform(put("/api/v1/purchases/{id}", PURCHASE_ID)
        .with(readonly())
        .header("X-Company-Id", COMPANY_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"RECEIVED\"}"))
      .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("POST /api/v1/purchases/{id}/cancel - ERP_USER cannot cancel (403 Forbidden)")
  void testCancelPurchase_ErpUserRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/purchases/{id}/cancel", PURCHASE_ID)
        .with(erpUser())
        .header("X-Company-Id", COMPANY_ID))
      .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("POST /api/v1/purchases/{id}/cancel - SETTINGS cannot cancel (403 Forbidden)")
  void testCancelPurchase_SettingsRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/purchases/{id}/cancel", PURCHASE_ID)
        .with(settings())
        .header("X-Company-Id", COMPANY_ID))
      .andExpect(status().isForbidden());
  }
}
