package com.datakomerz.pymes.sales;

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
 * Authorization tests for SalesController.
 * Validates RBAC rules for operational endpoints:
 * - GET: All roles (ADMIN, SETTINGS, ERP_USER, READONLY)
 * - POST/PUT operational: ADMIN, ERP_USER
 * - DELETE: ADMIN only
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SalesControllerAuthTest {

  @Autowired
  private MockMvc mockMvc;

  private static final UUID SALE_ID = UUID.randomUUID();

  @Test
  @WithMockUser(roles = "READONLY")
  @DisplayName("POST /api/v1/sales - READONLY cannot create sale (403 Forbidden)")
  void testCreateSale_ReadonlyRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/sales")
        .header("X-Company-Id", UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"customerId\":\"" + UUID.randomUUID() + "\"}"))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "SETTINGS")
  @DisplayName("POST /api/v1/sales - SETTINGS cannot create sale (403 Forbidden)")
  void testCreateSale_SettingsRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/sales")
        .header("X-Company-Id", UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"customerId\":\"" + UUID.randomUUID() + "\"}"))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "READONLY")
  @DisplayName("PUT /api/v1/sales/{id} - READONLY cannot update sale (403 Forbidden)")
  void testUpdateSale_ReadonlyRole_Forbidden() throws Exception {
    mockMvc.perform(put("/api/v1/sales/{id}", SALE_ID)
        .header("X-Company-Id", UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"COMPLETED\"}"))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "SETTINGS")
  @DisplayName("DELETE /api/v1/sales/{id} - SETTINGS cannot delete (403 Forbidden)")
  void testDeleteSale_SettingsRole_Forbidden() throws Exception {
    mockMvc.perform(delete("/api/v1/sales/{id}", SALE_ID)
        .header("X-Company-Id", UUID.randomUUID().toString()))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ERP_USER")
  @DisplayName("DELETE /api/v1/sales/{id} - ERP_USER cannot delete (403 Forbidden)")
  void testDeleteSale_ErpUserRole_Forbidden() throws Exception {
    mockMvc.perform(delete("/api/v1/sales/{id}", SALE_ID)
        .header("X-Company-Id", UUID.randomUUID().toString()))
      .andExpect(status().isForbidden());
  }
}
