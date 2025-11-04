package com.datakomerz.pymes.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security tests for Sprint 1 critical fixes:
 * - Actuator endpoints protection
 * - CORS configuration validation
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(com.datakomerz.pymes.config.TestJwtDecoderConfig.class)
class SecurityConfigActuatorTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void actuatorHealthShouldBePublic() throws Exception {
    mockMvc.perform(get("/actuator/health"))
      .andExpect(status().isOk());
  }

  @Test
  void actuatorInfoShouldBePublic() throws Exception {
    mockMvc.perform(get("/actuator/info"))
      .andExpect(status().isOk());
  }

  @Test
  void actuatorMetricsShouldRequireAuthentication() throws Exception {
    mockMvc.perform(get("/actuator/metrics"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void actuatorPrometheusShouldRequireAuthentication() throws Exception {
    mockMvc.perform(get("/actuator/prometheus"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void actuatorEnvShouldRequireAuthentication() throws Exception {
    mockMvc.perform(get("/actuator/env"))
      .andExpect(status().isUnauthorized());
  }
}
