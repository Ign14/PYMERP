package com.datakomerz.pymes.billing.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.billing.persistence.ContingencyQueueItem;
import com.company.billing.persistence.ContingencyQueueItemRepository;
import com.company.billing.persistence.ContingencyQueueStatus;
import com.company.billing.persistence.DocumentFile;
import com.company.billing.persistence.DocumentFileRepository;
import com.company.billing.persistence.DocumentFileVersion;
import com.company.billing.persistence.FiscalDocument;
import com.company.billing.persistence.FiscalDocumentRepository;
import com.company.billing.persistence.FiscalDocumentStatus;
import com.datakomerz.pymes.billing.dto.IssueInvoiceRequest;
import com.datakomerz.pymes.billing.dto.InvoiceResponse;
import com.datakomerz.pymes.billing.job.ContingencySyncJob;
import com.datakomerz.pymes.billing.provider.BillingProvider;
import com.datakomerz.pymes.billing.provider.BillingProvider.IssueInvoiceResult;
import com.datakomerz.pymes.billing.service.BillingStorageService;
import com.datakomerz.pymes.billing.service.BillingWebhookFileClient;
import com.datakomerz.pymes.billing.support.FixtureLoader;
import com.datakomerz.pymes.billing.support.InvoicePayloadFixtures;
import com.datakomerz.pymes.billing.render.LocalInvoiceRenderer;
import com.datakomerz.pymes.billing.render.StubLocalInvoiceRenderer;
import com.datakomerz.pymes.billing.config.BillingWebhookProperties;
import com.datakomerz.pymes.billing.security.BillingWebhookSignatureVerifier;
import com.datakomerz.pymes.sales.Sale;
import com.datakomerz.pymes.sales.SaleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "billing.offline.enabled=true"
})
@WithMockUser(roles = "ERP_USER")
class BillingOfflineFlowIT {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private SaleRepository saleRepository;

  @Autowired
  private FiscalDocumentRepository fiscalDocumentRepository;

  @Autowired
  private DocumentFileRepository documentFileRepository;

  @Autowired
  private ContingencyQueueItemRepository contingencyQueueItemRepository;

  @Autowired
  private ContingencySyncJob contingencySyncJob;

  @Autowired
  private TestConfig.TestBillingProvider billingProvider;

  @Autowired
  private TestConfig.InMemoryBillingStorageService storageService;

  @Autowired
  private TestConfig.TestWebhookFileClient webhookFileClient;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private Sale sale;

  @BeforeEach
  void setUp() {
    jdbcTemplate.execute("""
        CREATE TABLE IF NOT EXISTS contingency_queue_items (
          id UUID PRIMARY KEY,
          document_id UUID NOT NULL,
          idempotency_key VARCHAR(100) NOT NULL,
          provider_payload CLOB NOT NULL,
          status VARCHAR(24) NOT NULL,
          sync_attempts INT NOT NULL DEFAULT 0,
          last_sync_at TIMESTAMP,
          error_detail VARCHAR(255),
          encrypted_blob BYTEA,
          created_at TIMESTAMP NOT NULL,
          updated_at TIMESTAMP NOT NULL
        )
        """);
    jdbcTemplate.execute("""
        CREATE UNIQUE INDEX IF NOT EXISTS uq_contingency_idempotency
          ON contingency_queue_items(idempotency_key)
        """);
    webhookFileClient.clear();
    storageService.clear();
    billingProvider.reset();
    documentFileRepository.deleteAll();
    contingencyQueueItemRepository.deleteAll();
    fiscalDocumentRepository.deleteAll();
    saleRepository.deleteAll();

    sale = new Sale();
    sale.setCompanyId(UUID.randomUUID());
    sale.setStatus("PENDING");
    sale.setNet(new BigDecimal("10000"));
    sale.setVat(new BigDecimal("1900"));
    sale.setTotal(new BigDecimal("11900"));
    sale.setPaymentMethod("EFECTIVO");
    sale.setDocType("FACTURA");
    sale.setPaymentTermDays(30);
    saleRepository.saveAndFlush(sale);
  }

  @Test
  void offlineIssuanceFlow_reachesOfficialAfterWebhook() throws Exception {
    String idempotencyKey = "offline-sync-flow";
    IssueInvoiceRequest request = InvoicePayloadFixtures
        .issueInvoiceRequest("fixtures/billing/issue_invoice_offline.json", sale.getId(), idempotencyKey);

    MvcResult result = mockMvc.perform(post("/api/v1/billing/invoices")
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("OFFLINE_PENDING"))
        .andExpect(jsonPath("$.offline").value(true))
        .andExpect(jsonPath("$.links.localPdf").isNotEmpty())
        .andExpect(jsonPath("$.links.officialPdf").doesNotExist())
        .andReturn();

    InvoiceResponse invoiceResponse = objectMapper
        .readValue(result.getResponse().getContentAsString(), InvoiceResponse.class);
    UUID documentId = invoiceResponse.id();


    ContingencyQueueItem queueItem = contingencyQueueItemRepository.findByIdempotencyKey(idempotencyKey)
        .orElseThrow();
    assertThat(queueItem.getStatus()).isEqualTo(ContingencyQueueStatus.OFFLINE_PENDING);

    billingProvider.setNextResult(new IssueInvoiceResult(
        "demo-provider",
        "provider-doc-42",
        "track-555",
        "F001-000123",
        null));

    contingencySyncJob.processQueue();

    assertThat(contingencyQueueItemRepository.findById(queueItem.getId())).isEmpty();

    FiscalDocument syncedDocument = fiscalDocumentRepository.findById(documentId)
        .orElseThrow();
    assertThat(syncedDocument.getStatus()).isEqualTo(FiscalDocumentStatus.SENT);
    assertThat(syncedDocument.isOffline()).isFalse();
    assertThat(syncedDocument.getProvider()).isEqualTo("demo-provider");
    assertThat(syncedDocument.getTrackId()).isEqualTo("track-555");
    assertThat(syncedDocument.getNumber()).isEqualTo("F001-000123");
    assertThat(syncedDocument.getSyncAttempts()).isEqualTo(1);

    byte[] officialPdf = "official-pdf-content".getBytes(StandardCharsets.UTF_8);
    byte[] officialXml = "<xml>invoice</xml>".getBytes(StandardCharsets.UTF_8);
    ObjectNode webhookPayload = FixtureLoader.objectNode("fixtures/billing/webhook_accepted.json");
    webhookPayload.put("documentId", documentId.toString());
    webhookFileClient.register("https://provider.example/files/invoice-000123.pdf", officialPdf);
    webhookFileClient.register("https://provider.example/files/invoice-000123.xml", officialXml);

    mockMvc.perform(post("/webhooks/billing")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(webhookPayload)))
        .andExpect(status().isAccepted());

    FiscalDocument acceptedDocument = fiscalDocumentRepository.findById(documentId)
        .orElseThrow();
    assertThat(acceptedDocument.getStatus()).isEqualTo(FiscalDocumentStatus.ACCEPTED);
    assertThat(acceptedDocument.getNumber()).isEqualTo("F001-000123");
    assertThat(acceptedDocument.isOffline()).isFalse();

    List<DocumentFile> officialFiles = documentFileRepository
        .findByDocumentIdAndVersion(documentId, DocumentFileVersion.OFFICIAL);
    assertThat(officialFiles)
        .isNotEmpty()
        .anyMatch(file -> "application/pdf".equals(file.getContentType()));
    List<TestConfig.InMemoryBillingStorageService.StoredRecord> storedRecords =
        storageService.storedFor(documentId);
    assertThat(storedRecords.stream()
        .filter(record -> record.version() == DocumentFileVersion.OFFICIAL)
        .toList()).isNotEmpty();
  }

  @Test
  void webhookRejected_updatesDocumentWithErrorDetail() throws Exception {
    String idempotencyKey = "offline-rejected-flow";
    IssueInvoiceRequest request = InvoicePayloadFixtures
        .issueInvoiceRequest("fixtures/billing/issue_invoice_offline.json", sale.getId(), idempotencyKey);

    mockMvc.perform(post("/api/v1/billing/invoices")
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("OFFLINE_PENDING"));

    FiscalDocument initialDocument = fiscalDocumentRepository.findAll().stream()
        .findFirst()
        .orElseThrow();

    billingProvider.setNextResult(new IssueInvoiceResult(
        "demo-provider",
        "provider-doc-99",
        "track-999",
        "F001-000999",
        null));
    contingencySyncJob.processQueue();

    ObjectNode rejectedPayload = FixtureLoader.objectNode("fixtures/billing/webhook_rejected.json");
    rejectedPayload.put("documentId", initialDocument.getId().toString());
    mockMvc.perform(post("/webhooks/billing")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(rejectedPayload)))
        .andExpect(status().isAccepted());

    FiscalDocument rejectedDocument = fiscalDocumentRepository.findById(initialDocument.getId())
        .orElseThrow();
    assertThat(rejectedDocument.getStatus()).isEqualTo(FiscalDocumentStatus.REJECTED);
    assertThat(rejectedDocument.getErrorDetail()).contains("Numero duplicado");
    assertThat(rejectedDocument.getErrorDetail()).contains("Documento fuera de rango");
  }

  @TestConfiguration
  static class TestConfig {

    @Bean
    @Primary
    TestBillingProvider billingProvider() {
      return new TestBillingProvider();
    }

    @Bean
    @Primary
    InMemoryBillingStorageService billingStorageService() {
      return new InMemoryBillingStorageService();
    }

    @Bean
    @Primary
    TestWebhookFileClient billingWebhookFileClient() {
      return new TestWebhookFileClient();
    }

    @Bean
    @Primary
    LocalInvoiceRenderer localInvoiceRenderer() {
      return new StubLocalInvoiceRenderer();
    }

    @Bean
    @Primary
    Clock fixedClock() {
      return Clock.fixed(Instant.parse("2025-01-15T10:00:00Z"), ZoneOffset.UTC);
    }

    @Bean
    @Primary
    BillingWebhookSignatureVerifier billingWebhookSignatureVerifier(
        BillingWebhookProperties properties,
        Clock clock) {
      return new BillingWebhookSignatureVerifier(properties, clock) {
        @Override
        public void verify(String signatureHeader, String requestBody) {
          // no-op for integration tests
        }
      };
    }

    @Bean
    @Primary
    JwtDecoder jwtDecoder(Clock clock) {
      return token -> Jwt.withTokenValue(token)
          .headers(headers -> headers.put("alg", "none"))
          .issuedAt(clock.instant())
          .expiresAt(clock.instant().plusSeconds(3600))
          .claim("sub", "test-user")
          .claim("scope", "test")
          .build();
    }

    @Bean
    MeterRegistry meterRegistry() {
      return new SimpleMeterRegistry();
    }

    static class TestBillingProvider implements BillingProvider {

      private IssueInvoiceResult nextResult;
      private String lastIdempotencyKey;

      void setNextResult(IssueInvoiceResult result) {
        this.nextResult = result;
      }

      void reset() {
        this.nextResult = null;
        this.lastIdempotencyKey = null;
      }

      @Override
      public IssueInvoiceResult issueInvoice(com.datakomerz.pymes.billing.model.InvoicePayload payload,
                                             String idempotencyKey) {
        this.lastIdempotencyKey = idempotencyKey;
        if (nextResult == null) {
          throw new IllegalStateException("No provider result configured for test");
        }
        IssueInvoiceResult result = nextResult;
        nextResult = null;
        return result;
      }

      @Override
      public ProviderDocument fetchDocument(String providerDocumentId) {
        throw new UnsupportedOperationException("Not implemented for tests");
      }
    }

    static class InMemoryBillingStorageService implements BillingStorageService {

      private final Map<UUID, List<StoredRecord>> stored = new ConcurrentHashMap<>();
      private final AtomicInteger counter = new AtomicInteger();

      @Override
      public StoredFile store(UUID documentId,
                              com.company.billing.persistence.DocumentFileKind kind,
                              DocumentFileVersion version,
                              byte[] content,
                              String filename,
                              String contentType) {
        String storageKey = "memory/" + kind.name().toLowerCase() + "/" + documentId + "/"
            + version.name().toLowerCase() + "-" + counter.incrementAndGet()
            + (filename != null && filename.toLowerCase().endsWith(".xml") ? ".xml" : ".pdf");
        String checksum = Integer.toHexString(Arrays.hashCode(content));
        stored.computeIfAbsent(documentId, id -> new ArrayList<>())
            .add(new StoredRecord(kind, version, storageKey, checksum, contentType, content));
        return new StoredFile(storageKey, checksum);
      }

      @Override
      public byte[] read(String storageKey) {
        return stored.values().stream()
            .flatMap(List::stream)
            .filter(record -> record.storageKey().equals(storageKey))
            .findFirst()
            .map(StoredRecord::content)
            .orElseThrow(() -> new IllegalArgumentException("No stored file for key " + storageKey));
      }

      @Override
      public org.springframework.core.io.Resource loadAsResource(String storageKey) {
        byte[] data = read(storageKey);
        return new org.springframework.core.io.ByteArrayResource(data) {
          @Override
          public String getFilename() {
            return storageKey.substring(storageKey.lastIndexOf('/') + 1);
          }
        };
      }

      List<StoredRecord> storedFor(UUID documentId) {
        return stored.getOrDefault(documentId, List.of());
      }

      void clear() {
        stored.clear();
        counter.set(0);
      }

      record StoredRecord(com.company.billing.persistence.DocumentFileKind kind,
                          DocumentFileVersion version,
                          String storageKey,
                          String checksum,
                          String contentType,
                          byte[] content) {
      }
    }

    static class TestWebhookFileClient implements BillingWebhookFileClient {

      private final Map<String, byte[]> files = new ConcurrentHashMap<>();

      void register(String url, byte[] content) {
        files.put(url, content);
      }

      void clear() {
        files.clear();
      }

      @Override
      public byte[] download(String url) {
        byte[] content = files.get(url);
        if (content == null) {
          throw new IllegalStateException("No fixture registered for url " + url);
        }
        return content;
      }
    }
  }
}
