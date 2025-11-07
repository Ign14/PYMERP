package com.datakomerz.pymes.controller;

import static com.datakomerz.pymes.testsupport.AuthTestUtils.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

/**
 * Authorization tests for CustomerSegmentController.
 * Validates RBAC rules for segment management:
 * - GET: All roles
 * - POST/PUT: ADMIN, SETTINGS
 * - DELETE: ADMIN only
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.datakomerz.pymes.config.TestJwtDecoderConfig.class)
class CustomerSegmentControllerAuthTest {

  @Autowired
  private MockMvc mockMvc;

  private static final UUID SEGMENT_ID = UUID.randomUUID();
  private static final String COMPANY_ID = "00000000-0000-0000-0000-000000000001";

  @Test
  @DisplayName("POST /api/v1/segments - ERP_USER cannot create (403 Forbidden)")
  void testCreateSegment_ErpUserRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/customer-segments")
        .with(erpUser())
        .header("X-Company-Id", COMPANY_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "code": "VIP",
            "name": "VIP Customers",
            "description": "Premium segment",
            "color": "#FFAA00",
            "active": true
          }
          """))
      .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("PUT /api/v1/segments/{id} - READONLY cannot update (403 Forbidden)")
  void testUpdateSegment_ReadonlyRole_Forbidden() throws Exception {
    mockMvc.perform(put("/api/v1/customer-segments/{id}", SEGMENT_ID)
        .with(readonly())
        .header("X-Company-Id", COMPANY_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "code": "VIP",
            "name": "Premium Customers",
            "description": "Updated segment",
            "color": "#00AACC",
            "active": true
          }
          """))
      .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("DELETE /api/v1/segments/{id} - SETTINGS cannot delete (403 Forbidden)")
  void testDeleteSegment_SettingsRole_Forbidden() throws Exception {
    mockMvc.perform(delete("/api/v1/customer-segments/{id}", SEGMENT_ID)
        .with(settings())
        .header("X-Company-Id", COMPANY_ID))
      .andExpect(status().isForbidden());
  }
}
