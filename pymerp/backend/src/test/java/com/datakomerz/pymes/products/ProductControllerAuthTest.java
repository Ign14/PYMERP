package com.datakomerz.pymes.products;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.datakomerz.pymes.testsupport.AuthTestUtils.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
  private static final String COMPANY_ID = "00000000-0000-0000-0000-000000000001";

  @Test
  @DisplayName("POST /api/v1/products - ERP_USER cannot create (403 Forbidden)")
  void testCreateProduct_ErpUserRole_Forbidden() throws Exception {
    String productJson = "{\"sku\":\"SKU-001\",\"name\":\"New Product\"}";
    MockMultipartFile productPart = new MockMultipartFile(
        "product", "", MediaType.APPLICATION_JSON_VALUE, productJson.getBytes(StandardCharsets.UTF_8));

    mockMvc.perform(multipart("/api/v1/products")
        .file(productPart)
        .with(erpUser())
        .header("X-Company-Id", COMPANY_ID)
        .contentType(MediaType.MULTIPART_FORM_DATA))
      .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("POST /api/v1/products - READONLY cannot create (403 Forbidden)")
  void testCreateProduct_ReadonlyRole_Forbidden() throws Exception {
    String productJson = "{\"sku\":\"SKU-001\",\"name\":\"New Product\"}";
    MockMultipartFile productPart = new MockMultipartFile(
        "product", "", MediaType.APPLICATION_JSON_VALUE, productJson.getBytes(StandardCharsets.UTF_8));

    mockMvc.perform(multipart("/api/v1/products")
        .file(productPart)
        .with(readonly())
        .header("X-Company-Id", COMPANY_ID)
        .contentType(MediaType.MULTIPART_FORM_DATA))
      .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("DELETE /api/v1/products/{id} - SETTINGS cannot delete (403 Forbidden)")
  void testDeleteProduct_SettingsRole_Forbidden() throws Exception {
    mockMvc.perform(delete("/api/v1/products/{id}", PRODUCT_ID)
        .with(settings())
        .header("X-Company-Id", COMPANY_ID))
      .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("DELETE /api/v1/products/{id} - ERP_USER cannot delete (403 Forbidden)")
  void testDeleteProduct_ErpUserRole_Forbidden() throws Exception {
    mockMvc.perform(delete("/api/v1/products/{id}", PRODUCT_ID)
        .with(erpUser())
        .header("X-Company-Id", COMPANY_ID))
      .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("DELETE /api/v1/products/{id} - READONLY cannot delete (403 Forbidden)")
  void testDeleteProduct_ReadonlyRole_Forbidden() throws Exception {
    mockMvc.perform(delete("/api/v1/products/{id}", PRODUCT_ID)
        .with(readonly())
        .header("X-Company-Id", COMPANY_ID))
      .andExpect(status().isForbidden());
  }
}
