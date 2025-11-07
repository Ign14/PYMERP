package com.datakomerz.pymes.pricing;

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
 * Authorization tests for PricingController.
 * Validates RBAC rules for pricing configuration:
 * - GET: All roles
 * - POST: ADMIN, SETTINGS
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PricingControllerAuthTest {

  @Autowired
  private MockMvc mockMvc;
  private static final String COMPANY_ID = "00000000-0000-0000-0000-000000000001";
  private static final java.util.UUID PRODUCT_ID = java.util.UUID.randomUUID();

  @Test
  @DisplayName("POST /api/v1/pricing/tiers - ERP_USER cannot create (403 Forbidden)")
  void testCreatePricingTier_ErpUserRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/products/{productId}/prices", PRODUCT_ID)
        .with(erpUser())
        .header("X-Company-Id", COMPANY_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"price\": 10.0, \"validFrom\": \"2025-01-01T00:00:00Z\"}"))
      .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("POST /api/v1/pricing/rules - READONLY cannot create (403 Forbidden)")
  void testCreatePricingRule_ReadonlyRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/products/{productId}/prices", PRODUCT_ID)
        .with(readonly())
        .header("X-Company-Id", COMPANY_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"price\": 10.0, \"validFrom\": \"2025-01-01T00:00:00Z\"}"))
      .andExpect(status().isForbidden());
  }
}
