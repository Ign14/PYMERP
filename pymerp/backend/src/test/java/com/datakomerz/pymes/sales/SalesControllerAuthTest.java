package com.datakomerz.pymes.sales;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.datakomerz.pymes.testsupport.AuthTestUtils.*;

import java.math.BigDecimal;
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
  private static final String COMPANY_ID = "00000000-0000-0000-0000-000000000001";

  @Test
  @DisplayName("POST /api/v1/sales - READONLY cannot create sale (403 Forbidden)")
  void testCreateSale_ReadonlyRole_Forbidden() throws Exception {
    String body = "{" +
        "\"customerId\":\"" + UUID.randomUUID() + "\"," +
        "\"items\":[{" +
        "\"productId\":\"" + UUID.randomUUID() + "\"," +
        "\"qty\":1.0," +
        "\"unitPrice\":" + BigDecimal.ONE +
        "}]" +
        "}";

    mockMvc.perform(post("/api/v1/sales")
        .with(readonly())
        .header("X-Company-Id", COMPANY_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
      .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("POST /api/v1/sales - SETTINGS cannot create sale (403 Forbidden)")
  void testCreateSale_SettingsRole_Forbidden() throws Exception {
    String body = "{" +
        "\"customerId\":\"" + UUID.randomUUID() + "\"," +
        "\"items\":[{" +
        "\"productId\":\"" + UUID.randomUUID() + "\"," +
        "\"qty\":1.0," +
        "\"unitPrice\":" + BigDecimal.ONE +
        "}]" +
        "}";

    mockMvc.perform(post("/api/v1/sales")
        .with(settings())
        .header("X-Company-Id", COMPANY_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
      .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("PUT /api/v1/sales/{id} - READONLY cannot update sale (403 Forbidden)")
  void testUpdateSale_ReadonlyRole_Forbidden() throws Exception {
    mockMvc.perform(put("/api/v1/sales/{id}", SALE_ID)
        .with(readonly())
        .header("X-Company-Id", COMPANY_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"COMPLETED\"}"))
      .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("POST /api/v1/sales/{id}/cancel - SETTINGS cannot cancel (403 Forbidden)")
  void testCancelSale_SettingsRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/sales/{id}/cancel", SALE_ID)
        .with(settings())
        .header("X-Company-Id", COMPANY_ID))
      .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("POST /api/v1/sales/{id}/cancel - ERP_USER cannot cancel (403 Forbidden)")
  void testCancelSale_ErpUserRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/sales/{id}/cancel", SALE_ID)
        .with(erpUser())
        .header("X-Company-Id", COMPANY_ID))
      .andExpect(status().isForbidden());
  }
}
