package com.datakomerz.pymes.auth;

import com.datakomerz.pymes.auth.dto.AuthRequest;
import com.datakomerz.pymes.auth.dto.AuthResponse;
import com.datakomerz.pymes.auth.dto.RefreshRequest;
import com.datakomerz.pymes.company.Company;
import com.datakomerz.pymes.company.CompanyRepository;
import com.datakomerz.pymes.products.Product;
import com.datakomerz.pymes.products.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import com.datakomerz.pymes.config.TestJwtDecoderConfig;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
@TestPropertySource(properties = "app.security.jwt.oidc-enabled=false")
class AuthControllerIT {

  private static final String ADMIN_EMAIL = "admin@test.com";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private CompanyRepository companyRepository;

  @Autowired
  private UserAccountRepository userAccountRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private RefreshTokenRepository refreshTokenRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private UUID companyId;

  @BeforeEach
  void setUp() {
    productRepository.deleteAll();
    userAccountRepository.deleteAll();
    companyRepository.deleteAll();

    Company company = new Company();
    company.setId(UUID.randomUUID());
    company.setBusinessName("Test Company");
    company.setRut("76000000-0");
    companyRepository.save(company);
    this.companyId = company.getId();

    UserAccount admin = new UserAccount();
    admin.setCompanyId(companyId);
    admin.setEmail(ADMIN_EMAIL);
    admin.setName("Test Admin");
    admin.setRole("admin");
    admin.setStatus("active");
    admin.setRoles("ROLE_ADMIN,ROLE_ERP_USER");
    admin.setPasswordHash(passwordEncoder.encode("Secret123!"));
    userAccountRepository.save(admin);

    Product product = new Product();
    product.setCompanyId(companyId);
    product.setSku("SKU-001");
    product.setName("Producto Test");
    productRepository.save(product);
  }

  @Test
  void loginRefreshAndAccessProtectedEndpoint() throws Exception {
    AuthResponse authResponse = login();

    String refreshResponse = mockMvc.perform(post("/api/v1/auth/refresh")
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-Refresh-Token", authResponse.refreshToken())
        .header("X-Company-Id", companyId.toString())
        .content(objectMapper.writeValueAsString(new RefreshRequest(authResponse.refreshToken()))))
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString();

    AuthResponse refreshed = objectMapper.readValue(refreshResponse, AuthResponse.class);
    assertThat(refreshed.token()).isNotBlank();
    assertThat(refreshed.token()).isNotEqualTo(authResponse.token());
    assertThat(refreshed.refreshToken()).isNotEqualTo(authResponse.refreshToken());

    String productsResponse = mockMvc.perform(get("/api/v1/products")
        .header("Authorization", "Bearer " + refreshed.token())
        .header("X-Company-Id", companyId.toString()))
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString();

    JsonNode page = objectMapper.readTree(productsResponse);
    assertThat(page.get("content").isEmpty()).isFalse();
    assertThat(page.get("content").get(0).get("name").asText()).isEqualTo("Producto Test");
  }

  @Test
  void refreshFailsWhenUserDisabled() throws Exception {
    AuthResponse authResponse = login();

    UserAccount admin = userAccountRepository.findByEmailIgnoreCase(ADMIN_EMAIL)
      .orElseThrow();
    admin.setStatus("disabled");
    userAccountRepository.save(admin);

    mockMvc.perform(post("/api/v1/auth/refresh")
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-Refresh-Token", authResponse.refreshToken())
        .header("X-Company-Id", companyId.toString()))
      .andExpect(status().isForbidden())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.error").value("USER_DISABLED"));
  }

  @Test
  void refreshFailsWithInvalidTokenFormat() throws Exception {
    mockMvc.perform(post("/api/v1/auth/refresh")
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-Refresh-Token", "invalid-token")
        .header("X-Company-Id", companyId.toString()))
      .andExpect(status().isUnauthorized())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.error").value("TOKEN_INVALID"));
  }

  @Test
  void refreshFailsWhenTokenExpired() throws Exception {
    AuthResponse authResponse = login();

    UUID tokenId = extractTokenId(authResponse.refreshToken());
    RefreshToken refreshToken = refreshTokenRepository.findById(tokenId).orElseThrow();
    refreshToken.setExpiresAt(OffsetDateTime.now().minusMinutes(10));
    refreshTokenRepository.save(refreshToken);

    mockMvc.perform(post("/api/v1/auth/refresh")
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-Refresh-Token", authResponse.refreshToken())
        .header("X-Company-Id", companyId.toString()))
      .andExpect(status().isUnauthorized())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.error").value("TOKEN_INVALID"));
  }

  @Test
  void refreshFailsWhenTenantMismatch() throws Exception {
    AuthResponse authResponse = login();

    mockMvc.perform(post("/api/v1/auth/refresh")
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-Refresh-Token", authResponse.refreshToken())
        .header("X-Company-Id", UUID.randomUUID().toString()))
      .andExpect(status().isForbidden())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.error").value("TENANT_MISMATCH"));
  }

  @Test
  void loginFailsWithInvalidCredentials() throws Exception {
    AuthRequest request = new AuthRequest(ADMIN_EMAIL, "WrongPassword!");

    mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
        .header("X-Company-Id", companyId.toString()))
      .andExpect(status().isUnauthorized());
  }

  private AuthResponse login() throws Exception {
    AuthRequest request = new AuthRequest(ADMIN_EMAIL, "Secret123!");

    String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
        .header("X-Company-Id", companyId.toString()))
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString();

    return objectMapper.readValue(loginResponse, AuthResponse.class);
  }

  private UUID extractTokenId(String refreshToken) {
    String[] parts = refreshToken.split(":", 2);
    return UUID.fromString(parts[0]);
  }
}
