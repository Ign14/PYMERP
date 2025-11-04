package com.datakomerz.pymes.inventory;

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
 * Authorization tests for InventoryController.
 * Validates RBAC rules for inventory operations:
 * - GET: All roles
 * - POST/PUT operational: ADMIN, ERP_USER
 * - DELETE: ADMIN only
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryControllerAuthTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @WithMockUser(roles = "READONLY")
  @DisplayName("POST /api/v1/inventory/adjust - READONLY cannot adjust inventory (403 Forbidden)")
  void testAdjustInventory_ReadonlyRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/inventory/adjust")
        .header("X-Company-Id", UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"productId\":\"" + UUID.randomUUID() + "\",\"quantity\":10}"))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "SETTINGS")
  @DisplayName("POST /api/v1/inventory/adjust - SETTINGS cannot adjust (403 Forbidden)")
  void testAdjustInventory_SettingsRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/inventory/adjust")
        .header("X-Company-Id", UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"productId\":\"" + UUID.randomUUID() + "\",\"quantity\":10}"))
      .andExpect(status().isForbidden());
  }
}
