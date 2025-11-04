package com.datakomerz.pymes.purchases;

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

  @Test
  @WithMockUser(roles = "READONLY")
  @DisplayName("POST /api/v1/purchases - READONLY cannot create purchase (403 Forbidden)")
  void testCreatePurchase_ReadonlyRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/purchases")
        .header("X-Company-Id", UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"supplierId\":\"" + UUID.randomUUID() + "\"}"))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "SETTINGS")
  @DisplayName("POST /api/v1/purchases - SETTINGS cannot create purchase (403 Forbidden)")
  void testCreatePurchase_SettingsRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/purchases")
        .header("X-Company-Id", UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"supplierId\":\"" + UUID.randomUUID() + "\"}"))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "READONLY")
  @DisplayName("PUT /api/v1/purchases/{id}/receive - READONLY cannot receive (403 Forbidden)")
  void testReceivePurchase_ReadonlyRole_Forbidden() throws Exception {
    mockMvc.perform(put("/api/v1/purchases/{id}/receive", PURCHASE_ID)
        .header("X-Company-Id", UUID.randomUUID().toString()))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ERP_USER")
  @DisplayName("DELETE /api/v1/purchases/{id} - ERP_USER cannot delete (403 Forbidden)")
  void testDeletePurchase_ErpUserRole_Forbidden() throws Exception {
    mockMvc.perform(delete("/api/v1/purchases/{id}", PURCHASE_ID)
        .header("X-Company-Id", UUID.randomUUID().toString()))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "SETTINGS")
  @DisplayName("DELETE /api/v1/purchases/{id} - SETTINGS cannot delete (403 Forbidden)")
  void testDeletePurchase_SettingsRole_Forbidden() throws Exception {
    mockMvc.perform(delete("/api/v1/purchases/{id}", PURCHASE_ID)
        .header("X-Company-Id", UUID.randomUUID().toString()))
      .andExpect(status().isForbidden());
  }
}
