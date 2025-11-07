package com.datakomerz.pymes.customers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

/**
 * Authorization tests for CustomerController.
 * Validates RBAC rules defined in RBAC_MATRIX.md:
 * - GET endpoints: ADMIN, SETTINGS, ERP_USER, READONLY
 * - POST/PUT catalog: ADMIN, SETTINGS
 * - DELETE: ADMIN only
 * 
 * Note: These tests focus on HTTP status codes (403/401) validation,
 * not full integration. Database/service errors are expected and ignored.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.datakomerz.pymes.config.TestJwtDecoderConfig.class)
class CustomerControllerAuthTest {

  @Autowired
  private MockMvc mockMvc;

  private static final UUID CUSTOMER_ID = UUID.randomUUID();

  @Test
  @WithMockUser(roles = "READONLY")
  @DisplayName("POST /api/v1/customers - READONLY cannot create customer (403 Forbidden)")
  void testCreateCustomer_ReadonlyRole_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/customers")
        .header("X-Company-Id", UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"New Customer\"}"))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "READONLY")
  @DisplayName("POST /api/v1/customers - READONLY payload completo sigue prohibido (403 Forbidden)")
  void testCreateCustomer_ReadonlyRole_FullPayload_Forbidden() throws Exception {
    mockMvc.perform(post("/api/v1/customers")
        .header("X-Company-Id", UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "name": "Cliente Test",
              "rut": "12.345.678-9",
              "address": "Calle 123",
              "phone": "+56 9 1234 5678",
              "email": "cliente@test.com",
              "segment": "VIP",
              "contactPerson": "Juan Perez",
              "notes": "Sin notas",
              "active": true
            }
            """))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "SETTINGS")
  @DisplayName("DELETE /api/v1/customers/{id} - SETTINGS cannot delete (403 Forbidden)")
  void testDeleteCustomer_SettingsRole_Forbidden() throws Exception {
    mockMvc.perform(delete("/api/v1/customers/{id}", CUSTOMER_ID)
        .header("X-Company-Id", UUID.randomUUID().toString()))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ERP_USER")
  @DisplayName("DELETE /api/v1/customers/{id} - ERP_USER cannot delete (403 Forbidden)")
  void testDeleteCustomer_ErpUserRole_Forbidden() throws Exception {
    mockMvc.perform(delete("/api/v1/customers/{id}", CUSTOMER_ID)
        .header("X-Company-Id", UUID.randomUUID().toString()))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "READONLY")
  @DisplayName("DELETE /api/v1/customers/{id} - READONLY cannot delete (403 Forbidden)")
  void testDeleteCustomer_ReadonlyRole_Forbidden() throws Exception {
    mockMvc.perform(delete("/api/v1/customers/{id}", CUSTOMER_ID)
        .header("X-Company-Id", UUID.randomUUID().toString()))
      .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("GET /api/v1/customers - Anonymous user gets 401 Unauthorized")
  void testListCustomers_Anonymous_Unauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/customers"))
      .andExpect(status().isUnauthorized());
  }
}
