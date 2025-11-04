package com.datakomerz.pymes.billing.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.billing.persistence.DocumentFile;
import com.company.billing.persistence.DocumentFileKind;
import com.company.billing.persistence.DocumentFileRepository;
import com.company.billing.persistence.DocumentFileVersion;
import com.datakomerz.pymes.billing.service.BillingStorageService;
import com.datakomerz.pymes.company.Company;
import com.datakomerz.pymes.company.CompanyRepository;
import com.datakomerz.pymes.auth.UserAccount;
import com.datakomerz.pymes.auth.UserAccountRepository;
import com.datakomerz.pymes.auth.dto.AuthRequest;
import com.datakomerz.pymes.auth.dto.AuthResponse;
import com.datakomerz.pymes.config.TestJwtDecoderConfig;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.datakomerz.pymes.PymesApplication;

@SpringBootTest(classes = PymesApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
class BillingDownloadControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private DocumentFileRepository documentFileRepository;

  @Autowired
  private CompanyRepository companyRepository;

  @Autowired
  private UserAccountRepository userAccountRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private BillingStorageService storageService;

  private UUID documentId;
  private UUID companyId;
  private String accessToken;

  @BeforeEach
  void setUp() throws Exception {
    documentFileRepository.deleteAll();
    userAccountRepository.deleteAll();
    companyRepository.deleteAll();
    documentId = UUID.randomUUID();

    // Crear compañía y usuario admin para autenticación
    Company company = new Company();
    company.setBusinessName("Empresa Test");
    company.setRut("76000000-0");
    company.setBusinessActivity("Servicios");
    company.setAddress("Av. Test 123");
    company.setCommune("Santiago");
    company.setPhone("+56 2 1111 1111");
    company.setEmail("admin@test.cl");
    companyRepository.save(company);
    this.companyId = company.getId();

    UserAccount admin = new UserAccount();
    admin.setCompanyId(companyId);
    admin.setEmail("admin@companies.test");
    admin.setName("Admin Test");
    admin.setRole("admin");
    admin.setStatus("active");
    admin.setRoles("ROLE_ADMIN");
    admin.setPasswordHash(passwordEncoder.encode("Secret123!"));
    userAccountRepository.save(admin);

    this.accessToken = login(companyId, "admin@companies.test", "Secret123!");
  }

  private String login(UUID companyId, String email, String password) throws Exception {
    AuthRequest req = new AuthRequest(email, password);
    String resp = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Company-Id", companyId.toString())
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    AuthResponse auth = objectMapper.readValue(resp, AuthResponse.class);
    return auth.token();
  }

  @Test
  void downloadOfficialPdf_returnsStreamWithHeaders() throws Exception {
    // Arrange: persist an OFFICIAL PDF file record for a given document
  final DocumentFile file = new DocumentFile();
    file.setDocumentId(documentId);
    file.setKind(DocumentFileKind.FISCAL);
    file.setVersion(DocumentFileVersion.OFFICIAL);
    file.setContentType(MediaType.APPLICATION_PDF_VALUE);
    file.setStorageKey("storage/key/oficial.pdf");
    file.setChecksum("checksum-123");
  documentFileRepository.save(file);

    byte[] content = "%PDF-1.4 minimal".getBytes(StandardCharsets.ISO_8859_1);
    BDDMockito.given(storageService.loadAsResource("storage/key/oficial.pdf"))
        .willReturn(new ByteArrayResource(content));

    // Act + Assert
  mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/billing/documents/" + documentId + "/files/OFFICIAL")
      .header("Authorization", "Bearer " + accessToken)
      .header("X-Company-Id", companyId.toString()))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
        .andExpect(result -> {
          String disposition = result.getResponse().getHeader("Content-Disposition");
          assertThat(disposition).isNotNull();
          assertThat(disposition).contains("attachment");
          // El nombre del archivo se basa en el ID del DocumentFile devuelto por el controlador,
          // por lo que validamos que tenga extension.pdf, sin depender del ID exacto.
          assertThat(disposition).contains(".pdf");
          byte[] body = result.getResponse().getContentAsByteArray();
          assertThat(body).isNotEmpty();
        });
  }

  @Test
  void downloadOfficialXml_withQueryParam_returnsXml() throws Exception {
    // Arrange: two files (PDF and XML) for OFFICIAL; request contentType=xml
    DocumentFile pdf = new DocumentFile();
    pdf.setDocumentId(documentId);
    pdf.setKind(DocumentFileKind.FISCAL);
    pdf.setVersion(DocumentFileVersion.OFFICIAL);
    pdf.setContentType(MediaType.APPLICATION_PDF_VALUE);
    pdf.setStorageKey("storage/key/oficial.pdf");
    pdf.setChecksum("checksum-pdf");
    documentFileRepository.save(pdf);

  final DocumentFile xml = new DocumentFile();
    xml.setDocumentId(documentId);
    xml.setKind(DocumentFileKind.FISCAL);
    xml.setVersion(DocumentFileVersion.OFFICIAL);
    xml.setContentType(MediaType.APPLICATION_XML_VALUE);
    xml.setStorageKey("storage/key/oficial.xml");
    xml.setChecksum("checksum-xml");
  documentFileRepository.save(xml);

    byte[] xmlBytes = "<root/>".getBytes(StandardCharsets.UTF_8);
    BDDMockito.given(storageService.loadAsResource("storage/key/oficial.xml"))
        .willReturn(new ByteArrayResource(xmlBytes));

    mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/billing/documents/" + documentId + "/files/OFFICIAL")
      .param("contentType", "xml")
      .header("Authorization", "Bearer " + accessToken)
      .header("X-Company-Id", companyId.toString()))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
        .andExpect(result -> {
          String disposition = result.getResponse().getHeader("Content-Disposition");
          assertThat(disposition).isNotNull();
          assertThat(disposition).contains(".xml");
          assertThat(result.getResponse().getContentAsString()).contains("<root/>");
        });
  }
}
