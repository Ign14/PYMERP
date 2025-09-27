package com.datakomerz.pymes.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.datakomerz.pymes.auth.UserAccount;
import com.datakomerz.pymes.auth.UserAccountRepository;
import com.datakomerz.pymes.auth.dto.AuthRequest;
import com.datakomerz.pymes.auth.dto.AuthResponse;
import com.datakomerz.pymes.company.dto.CompanyRequest;
import com.datakomerz.pymes.company.dto.CompanyResponse;
import com.datakomerz.pymes.config.TestJwtDecoderConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
class CompanyControllerIT {

  private static final String ADMIN_EMAIL = "admin@companies.test";
  private static final String ADMIN_PASSWORD = "Secret123!";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private CompanyRepository companyRepository;

  @Autowired
  private UserAccountRepository userAccountRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private UUID companyId;
  private String accessToken;

  @BeforeEach
  void setUp() throws Exception {
    userAccountRepository.deleteAll();
    companyRepository.deleteAll();

    Company company = new Company();
    company.setBusinessName("Compañía Base");
    company.setRut("76000000-0");
    company.setBusinessActivity("Retail");
    company.setAddress("Av. Demo 123");
    company.setCommune("Santiago");
    company.setPhone("+56 2 1111 1111");
    company.setEmail("admin@base.cl");
    companyRepository.save(company);
    this.companyId = company.getId();

    UserAccount admin = new UserAccount();
    admin.setCompanyId(companyId);
    admin.setEmail(ADMIN_EMAIL);
    admin.setName("Admin Test");
    admin.setRole("admin");
    admin.setStatus("active");
    admin.setRoles("ROLE_ADMIN");
    admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
    userAccountRepository.save(admin);

    this.accessToken = login();
  }

  @Test
  void createsCompanyWithAllFields() throws Exception {
    CompanyRequest request = new CompanyRequest(
      "Nueva Compañía",
      "77.123.456-9",
      "Servicios",
      "Av. Central 456",
      "Providencia",
      "+56 9 2222 3333",
      "contacto@nueva.cl",
      "¡Gracias por tu compra!"
    );

    String responseJson = mockMvc.perform(post("/api/v1/companies")
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + accessToken)
        .header("X-Company-Id", companyId.toString())
        .content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.businessName").value("Nueva Compañía"))
      .andExpect(jsonPath("$.rut").value("77123456-9"))
      .andExpect(jsonPath("$.businessActivity").value("Servicios"))
      .andExpect(jsonPath("$.address").value("Av. Central 456"))
      .andExpect(jsonPath("$.commune").value("Providencia"))
      .andExpect(jsonPath("$.phone").value("+56 9 2222 3333"))
      .andExpect(jsonPath("$.email").value("contacto@nueva.cl"))
      .andExpect(jsonPath("$.receiptFooterMessage").value("¡Gracias por tu compra!"))
      .andExpect(jsonPath("$.createdAt").isNotEmpty())
      .andExpect(jsonPath("$.updatedAt").isNotEmpty())
      .andReturn()
      .getResponse()
      .getContentAsString();

    CompanyResponse response = objectMapper.readValue(responseJson, CompanyResponse.class);
    Company saved = companyRepository.findById(response.id()).orElseThrow();
    assertThat(saved.getBusinessName()).isEqualTo("Nueva Compañía");
    assertThat(saved.getRut()).isEqualTo("77123456-9");
    assertThat(saved.getBusinessActivity()).isEqualTo("Servicios");
    assertThat(saved.getEmail()).isEqualTo("contacto@nueva.cl");
  }

  @Test
  void updatesExistingCompany() throws Exception {
    Company existing = new Company();
    existing.setBusinessName("Ediciones Chile");
    existing.setRut("76111111-6");
    existing.setBusinessActivity("Editorial");
    existing.setEmail("ventas@ediciones.cl");
    companyRepository.save(existing);

    CompanyRequest update = new CompanyRequest(
      "Ediciones Chile Actualizada",
      "76.111.111-6",
      "Editorial y diseño",
      "Av. Alameda 1001",
      "Santiago",
      "+56 2 9999 0000",
      "contacto@ediciones.cl",
      "Factura electrónica"
    );

    mockMvc.perform(put("/api/v1/companies/" + existing.getId())
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + accessToken)
        .header("X-Company-Id", companyId.toString())
        .content(objectMapper.writeValueAsString(update)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.businessName").value("Ediciones Chile Actualizada"))
      .andExpect(jsonPath("$.rut").value("76111111-6"))
      .andExpect(jsonPath("$.businessActivity").value("Editorial y diseño"))
      .andExpect(jsonPath("$.email").value("contacto@ediciones.cl"));

    Company reloaded = companyRepository.findById(existing.getId()).orElseThrow();
    assertThat(reloaded.getBusinessName()).isEqualTo("Ediciones Chile Actualizada");
    assertThat(reloaded.getAddress()).isEqualTo("Av. Alameda 1001");
    assertThat(reloaded.getReceiptFooterMessage()).isEqualTo("Factura electrónica");
  }

  @Test
  void rejectsInvalidRut() throws Exception {
    CompanyRequest request = new CompanyRequest(
      "Compañía Inválida",
      "12.345.678-9",
      null,
      null,
      null,
      null,
      null,
      null
    );

    mockMvc.perform(post("/api/v1/companies")
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + accessToken)
        .header("X-Company-Id", companyId.toString())
        .content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.detail").value("Validation failed"))
      .andExpect(jsonPath("$.errors[0].field").value("rut"));
  }

  @Test
  void rejectsDuplicatedRut() throws Exception {
    CompanyRequest request = new CompanyRequest(
      "Otra Compañía",
      "76.000.000-0",
      null,
      null,
      null,
      null,
      null,
      null
    );

    mockMvc.perform(post("/api/v1/companies")
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + accessToken)
        .header("X-Company-Id", companyId.toString())
        .content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.detail").value("Ya existe una compañía con este RUT"));
  }

  @Test
  void listsCompanies() throws Exception {
    mockMvc.perform(get("/api/v1/companies")
        .header("Authorization", "Bearer " + accessToken)
        .header("X-Company-Id", companyId.toString()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].businessName").value("Compañía Base"))
      .andExpect(jsonPath("$[0].rut").value("76000000-0"));
  }

  private String login() throws Exception {
    AuthRequest request = new AuthRequest(ADMIN_EMAIL, ADMIN_PASSWORD);
    String response = mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-Company-Id", companyId.toString())
        .content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString();
    AuthResponse authResponse = objectMapper.readValue(response, AuthResponse.class);
    return authResponse.token();
  }
}
