package com.datakomerz.pymes.suppliers;

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
 * Authorization tests for SupplierController.
 * Validates RBAC rules for catalog management:
 * - GET: All roles
 * - POST/PUT: ADMIN, SETTINGS
 * - DELETE: ADMIN only
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SupplierControllerAuthTest {

  @Autowired
  private MockMvc mockMvc;

  private static final UUID SUPPLIER_ID = UUID.randomUUID();

  @Test
  @WithMockUser(roles = "ERP_USER")
  @DisplayName("POST /api/v1/suppliers - ERP_USER cannot create (403 Forbidden)")
  void testCreateSupplier_ErpUserRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/suppliers")
        .header("X-Company-Id", UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"New Supplier\"}"))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "READONLY")
  @DisplayName("PUT /api/v1/suppliers/{id} - READONLY cannot update (403 Forbidden)")
  void testUpdateSupplier_ReadonlyRole_Forbidden() throws Exception {
    mockMvc.perform(put("/api/v1/suppliers/{id}", SUPPLIER_ID)
        .header("X-Company-Id", UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"Updated Supplier\"}"))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "SETTINGS")
  @DisplayName("DELETE /api/v1/suppliers/{id} - SETTINGS cannot delete (403 Forbidden)")
  void testDeleteSupplier_SettingsRole_Forbidden() throws Exception {
    mockMvc.perform(delete("/api/v1/suppliers/{id}", SUPPLIER_ID)
        .header("X-Company-Id", UUID.randomUUID().toString()))
      .andExpect(status().isForbidden());
  }
}
