package com.datakomerz.pymes.billing.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import com.company.billing.persistence.DocumentFile;
import com.company.billing.persistence.DocumentFileKind;
import com.company.billing.persistence.DocumentFileRepository;
import com.company.billing.persistence.DocumentFileVersion;
import com.company.billing.persistence.FiscalDocument;
import com.company.billing.persistence.FiscalDocumentRepository;
import com.company.billing.persistence.FiscalDocumentStatus;
import com.company.billing.persistence.FiscalDocumentType;
import com.company.billing.persistence.TaxMode;
import com.datakomerz.pymes.billing.event.DocumentAccepted;
import com.datakomerz.pymes.billing.event.DocumentRejected;
import com.datakomerz.pymes.billing.service.BillingStorageService;
import com.datakomerz.pymes.billing.service.BillingWebhookFileClient;
import com.datakomerz.pymes.config.TestJwtDecoderConfig;
import com.datakomerz.pymes.sales.Sale;
import com.datakomerz.pymes.sales.SaleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest(properties = "billing.webhook.secret=test-secret")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestJwtDecoderConfig.class, BillingWebhookControllerTest.EventCaptureConfig.class})
class BillingWebhookControllerTest {

  private static final String SECRET = "test-secret";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private FiscalDocumentRepository fiscalDocumentRepository;

  @Autowired
  private DocumentFileRepository documentFileRepository;

  @Autowired
  private SaleRepository saleRepository;

  @Autowired
  private Clock clock;

  @Autowired
  private TestDocumentEventListener eventListener;

  @MockBean
  private BillingWebhookFileClient fileClient;

  @MockBean
  private BillingStorageService storageService;

  @BeforeEach
  void setUp() throws Exception {
    documentFileRepository.deleteAll();
    fiscalDocumentRepository.deleteAll();
    saleRepository.deleteAll();
    eventListener.reset();

    BDDMockito.given(storageService.store(any(), any(), any(), any(), any(), any()))
        .willAnswer(invocation -> {
          String filename = invocation.getArgument(4);
          return new BillingStorageService.StoredFile(
              "billing/" + filename,
              "checksum-" + filename);
        });
  }

  @Test
  void invalidSignatureReturnsUnauthorized() throws Exception {
    String body = objectMapper.writeValueAsString(Map.of(
        "documentId", UUID.randomUUID(),
        "status", "ACCEPTED"
    ));

    mockMvc.perform(MockMvcRequestBuilders.post("/webhooks/billing")
            .header("X-Signature", "t=123,v1=invalid")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized());
  }

  @Test
  void acceptedWebhookUpdatesDocumentAndStoresOfficialFiles() throws Exception {
    FiscalDocument document = createFiscalDocument();
    DocumentFile localFile = createLocalFile(document.getId());

    byte[] pdfBytes = "PDF-DATA".getBytes(StandardCharsets.UTF_8);
    byte[] xmlBytes = "<xml/>".getBytes(StandardCharsets.UTF_8);

    String pdfUrl = "https://billing.example.com/files/doc.pdf";
    String xmlUrl = "https://billing.example.com/files/doc.xml";

    BDDMockito.given(fileClient.download(pdfUrl)).willReturn(pdfBytes);
    BDDMockito.given(fileClient.download(xmlUrl)).willReturn(xmlBytes);

    Map<String, Object> payload = Map.of(
        "provider", "billing-inc",
        "externalId", "F001-000123",
        "documentId", document.getId().toString(),
        "status", "ACCEPTED",
        "trackId", "track-001",
        "links", Map.of(
            "pdf", pdfUrl,
            "xml", xmlUrl
        )
    );

    String body = objectMapper.writeValueAsString(payload);

    mockMvc.perform(MockMvcRequestBuilders.post("/webhooks/billing")
            .header("X-Signature", buildSignatureHeader(body))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(MockMvcResultMatchers.status().isAccepted());

    FiscalDocument updated = fiscalDocumentRepository.findById(document.getId()).orElseThrow();
    assertThat(updated.getStatus()).isEqualTo(FiscalDocumentStatus.ACCEPTED);
    assertThat(updated.getNumber()).isEqualTo("F001-000123");
    assertThat(updated.getErrorDetail()).isNull();
    assertThat(updated.getProvider()).isEqualTo("billing-inc");
    assertThat(updated.getTrackId()).isEqualTo("track-001");

    List<DocumentFile> officialFiles = documentFileRepository
        .findByDocumentIdAndVersion(document.getId(), DocumentFileVersion.OFFICIAL);
    assertThat(officialFiles).hasSize(2);
    assertThat(officialFiles)
        .allMatch(file -> file.getPreviousFile() != null && file.getPreviousFile().getId().equals(localFile.getId()));
    assertThat(officialFiles)
        .anyMatch(file -> "application/pdf".equals(file.getContentType()));
    assertThat(officialFiles)
        .anyMatch(file -> "application/xml".equals(file.getContentType()));

    assertThat(eventListener.acceptedEvents())
        .map(DocumentAccepted::documentId)
        .contains(document.getId());
  }

  @Test
  void rejectedWebhookUpdatesStatusAndCapturesErrors() throws Exception {
    FiscalDocument document = createFiscalDocument();

    Map<String, Object> payload = Map.of(
        "provider", "billing-inc",
        "externalId", "F001-000123",
        "documentId", document.getId().toString(),
        "status", "REJECTED",
        "trackId", "track-002",
        "errors", List.of("Invalid data", "Duplicate number")
    );

    String body = objectMapper.writeValueAsString(payload);

    mockMvc.perform(MockMvcRequestBuilders.post("/webhooks/billing")
            .header("X-Signature", buildSignatureHeader(body))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(MockMvcResultMatchers.status().isAccepted());

    FiscalDocument updated = fiscalDocumentRepository.findById(document.getId()).orElseThrow();
    assertThat(updated.getStatus()).isEqualTo(FiscalDocumentStatus.REJECTED);
    assertThat(updated.getErrorDetail()).isEqualTo("Invalid data; Duplicate number");

    assertThat(eventListener.rejectedEvents())
        .map(DocumentRejected::documentId)
        .contains(document.getId());
  }

  private FiscalDocument createFiscalDocument() {
    Sale sale = new Sale();
    sale.setCompanyId(UUID.randomUUID());
    sale.setStatus("PENDING");
    sale.setNet(BigDecimal.TEN);
    sale.setVat(BigDecimal.ONE);
    sale.setTotal(BigDecimal.valueOf(11));
    sale.setPaymentTermDays(30);
    saleRepository.save(sale);

    FiscalDocument document = new FiscalDocument();
    document.setSale(sale);
    document.setDocumentType(FiscalDocumentType.FACTURA);
    document.setTaxMode(TaxMode.AFECTA);
    document.setStatus(FiscalDocumentStatus.SENT);
    document.setProvisionalNumber("CTG-2025-00001");
    return fiscalDocumentRepository.save(document);
  }

  private DocumentFile createLocalFile(UUID documentId) {
    DocumentFile file = new DocumentFile();
    file.setDocumentId(documentId);
    file.setKind(DocumentFileKind.FISCAL);
    file.setVersion(DocumentFileVersion.LOCAL);
    file.setContentType("application/pdf");
    file.setStorageKey("billing/local.pdf");
    file.setChecksum("checksum");
    return documentFileRepository.save(file);
  }

  private String buildSignatureHeader(String payload) throws Exception {
    long timestamp = Instant.now(clock).getEpochSecond();
    String payloadToSign = timestamp + "." + payload;
    javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
    javax.crypto.spec.SecretKeySpec key = new javax.crypto.spec.SecretKeySpec(
        SECRET.getBytes(StandardCharsets.UTF_8),
        "HmacSHA256");
    mac.init(key);
    String signature = HexFormat.of().formatHex(mac.doFinal(payloadToSign.getBytes(StandardCharsets.UTF_8)));
    return "t=" + timestamp + ",v1=" + signature;
  }

  @TestConfiguration
  static class EventCaptureConfig {

    @Bean
    TestDocumentEventListener testDocumentEventListener() {
      return new TestDocumentEventListener();
    }
  }

  static class TestDocumentEventListener {

    private final CopyOnWriteArrayList<DocumentAccepted> accepted = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<DocumentRejected> rejected = new CopyOnWriteArrayList<>();

    @EventListener
    void onDocumentAccepted(DocumentAccepted event) {
      accepted.add(event);
    }

    @EventListener
    void onDocumentRejected(DocumentRejected event) {
      rejected.add(event);
    }

    List<DocumentAccepted> acceptedEvents() {
      return List.copyOf(accepted);
    }

    List<DocumentRejected> rejectedEvents() {
      return List.copyOf(rejected);
    }

    void reset() {
      accepted.clear();
      rejected.clear();
    }
  }
}
