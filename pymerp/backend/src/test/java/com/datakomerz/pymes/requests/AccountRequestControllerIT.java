package com.datakomerz.pymes.requests;

import com.datakomerz.pymes.common.captcha.SimpleCaptchaPayload;
import com.datakomerz.pymes.requests.dto.AccountRequestPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.datakomerz.pymes.config.TestJwtDecoderConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
class AccountRequestControllerIT {

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
  void createRequestFailsWhenCaptchaInvalid() throws Exception {
    AccountRequestPayload payload = new AccountRequestPayload(
      "12.345.678-5",
      "Usuario Demo",
      "Av. Demo 123",
      "demo@example.com",
      "Comercial Demo",
      "Secret123!",
      "Secret123!",
      new SimpleCaptchaPayload(4, 5, "10")
    );

    mockMvc.perform(post("/api/v1/requests")
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-Company-Id", "00000000-0000-0000-0000-000000000001")
        .content(objectMapper.writeValueAsString(payload)))
      .andExpect(status().isBadRequest())
      .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
      .andExpect(jsonPath("$.field").value("captcha.answer"))
      .andExpect(jsonPath("$.type").value("https://pymerp.cl/problems/captcha-invalid"));
  }

  @Test
  void createRequestSucceedsWhenCaptchaValid() throws Exception {
    AccountRequestPayload payload = new AccountRequestPayload(
      "12.345.678-5",
      "Usuario Demo",
      "Av. Demo 123",
      "demo@example.com",
      "Comercial Demo",
      "Secret123!",
      "Secret123!",
      new SimpleCaptchaPayload(4, 5, "9")
    );

    var result = mockMvc.perform(post("/api/v1/requests")
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-Company-Id", "00000000-0000-0000-0000-000000000001")
        .content(objectMapper.writeValueAsString(payload)))
      .andReturn();

    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(result.getResponse().getStatus()).isEqualTo(201);
    assertThat(body.path("message").asText()).contains("¡Muchas gracias!");
  }
}
