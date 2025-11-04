package com.datakomerz.pymes.locations;

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

  @Test
  @WithMockUser(roles = "ERP_USER")
  @DisplayName("POST /api/v1/locations - ERP_USER cannot create (403 Forbidden)")
  void testCreateLocation_ErpUserRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/locations")
        .header("X-Company-Id", UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"New Location\"}"))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "READONLY")
  @DisplayName("PUT /api/v1/locations/{id} - READONLY cannot update (403 Forbidden)")
  void testUpdateLocation_ReadonlyRole_Forbidden() throws Exception {
    mockMvc.perform(put("/api/v1/locations/{id}", LOCATION_ID)
        .header("X-Company-Id", UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"Updated Location\"}"))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "SETTINGS")
  @DisplayName("DELETE /api/v1/locations/{id} - SETTINGS cannot delete (403 Forbidden)")
  void testDeleteLocation_SettingsRole_Forbidden() throws Exception {
    mockMvc.perform(delete("/api/v1/locations/{id}", LOCATION_ID)
        .header("X-Company-Id", UUID.randomUUID().toString()))
      .andExpect(status().isForbidden());
  }
}
