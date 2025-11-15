package com.datakomerz.pymes.requests;

import com.datakomerz.pymes.requests.dto.AccountRequestPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.datakomerz.pymes.config.TestJwtDecoderConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
@TestPropertySource(properties = "app.security.captcha.enabled=false")
class AccountRequestControllerCaptchaDisabledIT {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private AccountRequestRepository repository;

  @BeforeEach
  void clean() {
    repository.deleteAll();
  }

  @Test
  void createRequestSucceedsWithoutCaptchaWhenDisabled() throws Exception {
    AccountRequestPayload payload = new AccountRequestPayload(
      "12.345.678-5",
      "Usuario Demo",
      "Av. Demo 123",
      "demo@example.com",
      "Comercial Demo",
      "Secret123!",
      "Secret123!",
      null
    );

    mockMvc.perform(post("/api/v1/requests")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(payload)))
      .andExpect(status().isCreated());
  }
}
