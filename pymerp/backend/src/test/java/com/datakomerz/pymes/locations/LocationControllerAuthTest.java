package com.datakomerz.pymes.locations;

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
 * Authorization tests for LocationController.
 * Validates RBAC rules for catalog management:
 * - GET: All roles
 * - POST/PUT: ADMIN, SETTINGS
 * - DELETE: ADMIN only
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LocationControllerAuthTest {

  @Autowired
  private MockMvc mockMvc;

  private static final UUID LOCATION_ID = UUID.randomUUID();
  private static final String COMPANY_ID = "00000000-0000-0000-0000-000000000001";

  @Test
  @DisplayName("POST /api/v1/locations - ERP_USER cannot create (403 Forbidden)")
  void testCreateLocation_ErpUserRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/locations")
        .with(erpUser())
        .header("X-Company-Id", COMPANY_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"New Location\"}"))
      .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("PUT /api/v1/locations/{id} - READONLY cannot update (403 Forbidden)")
  void testUpdateLocation_ReadonlyRole_Forbidden() throws Exception {
    mockMvc.perform(put("/api/locations/{id}", LOCATION_ID)
        .with(readonly())
        .header("X-Company-Id", COMPANY_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"Updated Location\"}"))
      .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("DELETE /api/v1/locations/{id} - SETTINGS cannot delete (403 Forbidden)")
  void testDeleteLocation_SettingsRole_Forbidden() throws Exception {
    mockMvc.perform(delete("/api/locations/{id}", LOCATION_ID)
        .with(settings())
        .header("X-Company-Id", COMPANY_ID))
      .andExpect(status().isForbidden());
  }
}
