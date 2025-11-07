package com.datakomerz.pymes.inventory;

import static com.datakomerz.pymes.testsupport.AuthTestUtils.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
 
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
  private static final String COMPANY_ID = "00000000-0000-0000-0000-000000000001";
  private static final java.util.UUID PRODUCT_ID = java.util.UUID.randomUUID();

  @Test
  @DisplayName("POST /api/v1/inventory/adjust - READONLY cannot adjust inventory (403 Forbidden)")
  void testAdjustInventory_ReadonlyRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/inventory/adjustments")
        .with(readonly())
        .header("X-Company-Id", COMPANY_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"productId\":\"" + PRODUCT_ID +
                 "\",\"quantity\":10.5,\"reason\":\"stock count\",\"direction\":\"increase\"}"))
      .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("POST /api/v1/inventory/adjust - SETTINGS cannot adjust (403 Forbidden)")
  void testAdjustInventory_SettingsRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/inventory/adjustments")
        .with(settings())
        .header("X-Company-Id", COMPANY_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"productId\":\"" + PRODUCT_ID +
                 "\",\"quantity\":10.5,\"reason\":\"stock count\",\"direction\":\"increase\"}"))
      .andExpect(status().isForbidden());
  }
}
