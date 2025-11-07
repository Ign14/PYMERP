package com.datakomerz.pymes.suppliers;

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
  private static final String COMPANY_ID = "00000000-0000-0000-0000-000000000001";

  @Test
  @DisplayName("POST /api/v1/suppliers - ERP_USER cannot create (403 Forbidden)")
  void testCreateSupplier_ErpUserRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/suppliers")
        .with(erpUser())
        .header("X-Company-Id", COMPANY_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"New Supplier\",\"rut\":\"12345678-9\"}"))
      .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("PUT /api/v1/suppliers/{id} - READONLY cannot update (403 Forbidden)")
  void testUpdateSupplier_ReadonlyRole_Forbidden() throws Exception {
    mockMvc.perform(put("/api/v1/suppliers/{id}", SUPPLIER_ID)
        .with(readonly())
        .header("X-Company-Id", COMPANY_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"Updated Supplier\",\"rut\":\"12345678-9\"}"))
      .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("DELETE /api/v1/suppliers/{id} - SETTINGS cannot delete (403 Forbidden)")
  void testDeleteSupplier_SettingsRole_Forbidden() throws Exception {
    mockMvc.perform(delete("/api/v1/suppliers/{id}", SUPPLIER_ID)
        .with(settings())
        .header("X-Company-Id", COMPANY_ID))
      .andExpect(status().isForbidden());
  }
}
