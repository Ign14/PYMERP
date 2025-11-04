package com.datakomerz.pymes.requests.api;

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

/**
 * Authorization tests for AccountRequestController.
 * Validates public access (permitAll) for account registration:
 * - POST: Public access (no authentication required)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountRequestControllerAuthTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @DisplayName("POST /api/v1/requests - Anonymous can register (permitAll)")
  void testCreateAccountRequest_Anonymous_Success() throws Exception {
    mockMvc.perform(post("/api/v1/requests")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"test@example.com\",\"companyName\":\"Test Corp\"}"))
      .andExpect(status().isOk());
  }
}
