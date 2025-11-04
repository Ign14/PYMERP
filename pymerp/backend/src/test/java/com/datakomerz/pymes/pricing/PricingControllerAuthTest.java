package com.datakomerz.pymes.pricing;

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

  @Test
  @WithMockUser(roles = "ERP_USER")
  @DisplayName("POST /api/v1/pricing/tiers - ERP_USER cannot create (403 Forbidden)")
  void testCreatePricingTier_ErpUserRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/pricing/tiers")
        .header("X-Company-Id", UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"Premium\"}"))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "READONLY")
  @DisplayName("POST /api/v1/pricing/rules - READONLY cannot create (403 Forbidden)")
  void testCreatePricingRule_ReadonlyRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/pricing/rules")
        .header("X-Company-Id", UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"ruleType\":\"DISCOUNT\"}"))
      .andExpect(status().isForbidden());
  }
}
