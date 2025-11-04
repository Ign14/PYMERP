package com.datakomerz.pymes.products;

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
 * Authorization tests for ProductController.
 * Validates RBAC rules:
 * - GET: All roles (ADMIN, SETTINGS, ERP_USER, READONLY)
 * - POST/PUT: ADMIN, SETTINGS (catalog management)
 * - DELETE: ADMIN only
 * 
 * Note: These tests focus on HTTP status codes (403/401) validation,
 * not full integration. Database/service errors are expected and ignored.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerAuthTest {

  @Autowired
  private MockMvc mockMvc;

  private static final UUID PRODUCT_ID = UUID.randomUUID();

  @Test
  @WithMockUser(roles = "ERP_USER")
  @DisplayName("POST /api/v1/products - ERP_USER cannot create (403 Forbidden)")
  void testCreateProduct_ErpUserRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/products")
        .header("X-Company-Id", UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"New Product\"}"))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "READONLY")
  @DisplayName("POST /api/v1/products - READONLY cannot create (403 Forbidden)")
  void testCreateProduct_ReadonlyRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/products")
        .header("X-Company-Id", UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"New Product\"}"))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "SETTINGS")
  @DisplayName("DELETE /api/v1/products/{id} - SETTINGS cannot delete (403 Forbidden)")
  void testDeleteProduct_SettingsRole_Forbidden() throws Exception {
    mockMvc.perform(delete("/api/v1/products/{id}", PRODUCT_ID)
        .header("X-Company-Id", UUID.randomUUID().toString()))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ERP_USER")
  @DisplayName("DELETE /api/v1/products/{id} - ERP_USER cannot delete (403 Forbidden)")
  void testDeleteProduct_ErpUserRole_Forbidden() throws Exception {
    mockMvc.perform(delete("/api/v1/products/{id}", PRODUCT_ID)
        .header("X-Company-Id", UUID.randomUUID().toString()))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "READONLY")
  @DisplayName("DELETE /api/v1/products/{id} - READONLY cannot delete (403 Forbidden)")
  void testDeleteProduct_ReadonlyRole_Forbidden() throws Exception {
    mockMvc.perform(delete("/api/v1/products/{id}", PRODUCT_ID)
        .header("X-Company-Id", UUID.randomUUID().toString()))
      .andExpect(status().isForbidden());
  }
}
