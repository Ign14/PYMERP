package com.datakomerz.pymes.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.billing.persistence.ContingencyQueueItemRepository;
import com.company.billing.persistence.DocumentFileKind;
import com.company.billing.persistence.DocumentFileVersion;
import com.company.billing.persistence.FiscalDocument;
import com.company.billing.persistence.FiscalDocumentRepository;
import com.company.billing.persistence.FiscalDocumentStatus;
import com.company.billing.persistence.NonFiscalDocumentType;
import com.datakomerz.pymes.billing.model.InvoicePayload;
import com.datakomerz.pymes.billing.provider.BillingProvider;
import com.datakomerz.pymes.billing.provider.BillingProviderException;
import com.datakomerz.pymes.billing.provider.BillingProvider.OfficialDocument;
import com.datakomerz.pymes.billing.provider.BillingProvider.IssueInvoiceResult;
import com.datakomerz.pymes.billing.render.LocalInvoiceRenderer;
import com.datakomerz.pymes.billing.render.StubLocalInvoiceRenderer;
import com.datakomerz.pymes.billing.service.BillingDocumentView.DocumentLinks;
import com.datakomerz.pymes.billing.service.BillingIdempotencyStore;
import com.datakomerz.pymes.billing.support.InvoicePayloadFixtures;
import com.datakomerz.pymes.sales.Sale;
import com.datakomerz.pymes.sales.SaleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.datakomerz.pymes.multitenancy.TenantContext;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@Import({BillingService.class, BillingServiceTest.TestConfig.class})
class BillingServiceTest {

  @Autowired
  private BillingService billingService;

  @Autowired
  private SaleRepository saleRepository;

  @Autowired
  private ContingencyQueueItemRepository contingencyQueueItemRepository;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private TestBillingProvider billingProvider;

  @Autowired
  private FiscalDocumentRepository fiscalDocumentRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private Sale sale;

  private static final String DEFAULT_PAYLOAD_HASH = "payload-hash";

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
          encrypted_blob BLOB,
          created_at TIMESTAMP NOT NULL,
          updated_at TIMESTAMP NOT NULL
        )
        """);
    jdbcTemplate.execute("""
        CREATE UNIQUE INDEX IF NOT EXISTS uq_contingency_idempotency
          ON contingency_queue_items(idempotency_key)
        """);

    sale = new Sale();
    sale.setCompanyId(UUID.randomUUID());
    sale.setStatus("PENDING");
    sale.setNet(new BigDecimal("10000"));
    sale.setVat(new BigDecimal("1900"));
    sale.setTotal(new BigDecimal("11900"));
    sale.setPaymentMethod("EFECTIVO");
    sale.setDocType("FACTURA");
    sale.setIssuedAt(OffsetDateTime.now());
    sale.setPaymentTermDays(30);
    saleRepository.saveAndFlush(sale);
    TenantContext.setTenantId(sale.getCompanyId());
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void issueInvoiceOffline_enqueuesDocumentAndReturnsLocalLink() {
    InvoicePayload payload = InvoicePayloadFixtures.fiscalOffline(sale.getId());

    var result = billingService.issueInvoice(
        true,
        "OFFLINE",
        payload,
        "invoice-offline-key",
        DEFAULT_PAYLOAD_HASH);
    BillingDocumentView view = result.document();

    assertThat(view.category()).isEqualTo(com.datakomerz.pymes.billing.model.DocumentCategory.FISCAL);
    assertThat(view.fiscalStatus()).isEqualTo(FiscalDocumentStatus.OFFLINE_PENDING);
    assertThat(view.offline()).isTrue();
    DocumentLinks links = view.links();
    assertThat(links.localPdf()).isNotBlank();
    assertThat(links.officialPdf()).isNull();
    assertThat(view.files()).hasSize(1);
    assertThat(contingencyQueueItemRepository.findByIdempotencyKey("invoice-offline-key"))
        .isPresent();
  }

  @Test
  void issueInvoiceOnline_callsProviderAndStoresOfficialVersion() {
    byte[] officialBytes = "official-pdf".getBytes(StandardCharsets.UTF_8);
    billingProvider.setNextResult(new IssueInvoiceResult(
        "demo-provider",
        "provider-doc-1",
        "track-777",
        "F001-000123",
        new OfficialDocument(officialBytes, "official.pdf", "application/pdf", null)
    ));

    InvoicePayload payload = InvoicePayloadFixtures.fiscalOnline(sale.getId());

    var result = billingService.issueInvoice(
        false,
        "GOOD",
        payload,
        "invoice-online-key",
        DEFAULT_PAYLOAD_HASH);
    BillingDocumentView view = result.document();

    assertThat(view.fiscalStatus()).isEqualTo(FiscalDocumentStatus.SENT);
    assertThat(view.offline()).isFalse();
    assertThat(view.provider()).isEqualTo("demo-provider");
    assertThat(view.trackId()).isEqualTo("track-777");
    assertThat(view.number()).isEqualTo("F001-000123");
    assertThat(view.files()).hasSize(2);
    assertThat(view.links().officialPdf()).isNotBlank();
    assertThat(billingProvider.getLastPayload()).isNotNull();
    assertThat(billingProvider.getLastIdempotencyKey()).isEqualTo("invoice-online-key");
  }

  @Test
  void issueInvoiceWhenProviderRejects_marksDocumentAsFailed() {
    billingProvider.setNextException(new BillingProviderException("Rule violation"));
    InvoicePayload payload = InvoicePayloadFixtures.fiscalOnline(sale.getId());

    assertThatThrownBy(() -> billingService.issueInvoice(
        false,
        "GOOD",
        payload,
        "invoice-failed-key",
        DEFAULT_PAYLOAD_HASH))
        .isInstanceOf(BillingProviderException.class)
        .hasMessageContaining("Billing provider rejected invoice");

    FiscalDocument document = fiscalDocumentRepository.findAll().stream()
        .findFirst()
        .orElseThrow();
    assertThat(document.getStatus()).isEqualTo(FiscalDocumentStatus.FAILED);
    assertThat(document.isOffline()).isFalse();
  }

  @Test
  void createNonFiscal_generatesReadyDocument() {
    InvoicePayload payload = new InvoicePayload(
        sale.getId(),
        null,
        NonFiscalDocumentType.COTIZACION,
        null,
        null,
        "device-2",
        "POS-2",
        newPayloadNode("non-fiscal"));

    var view = billingService.createNonFiscal(payload);

    assertThat(view.category()).isEqualTo(com.datakomerz.pymes.billing.model.DocumentCategory.NON_FISCAL);
    assertThat(view.nonFiscalStatus()).isEqualTo(com.company.billing.persistence.NonFiscalDocumentStatus.READY);
    assertThat(view.links().localPdf()).isNotBlank();
    assertThat(view.files()).hasSize(1);
    assertThat(view.number()).startsWith("NF-");
  }

  private ObjectNode newPayloadNode(String mode) {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("mode", mode);
    node.put("saleId", sale.getId().toString());
    return node;
  }

  @TestConfiguration
  static class TestConfig {

    @Bean
    @Primary
    ObjectMapper billingTestObjectMapper() {
      return new ObjectMapper();
    }

    @Bean
    BillingStorageService billingStorageService() {
      return new InMemoryBillingStorageService();
    }

    @Bean
    LocalInvoiceRenderer localInvoiceRenderer() {
      return new StubLocalInvoiceRenderer();
    }

    @Bean
    Clock clock() {
      return Clock.fixed(Instant.parse("2025-01-15T10:00:00Z"), ZoneOffset.UTC);
    }

    @Bean
    TestBillingProvider billingProvider() {
      return new TestBillingProvider();
    }

    @Bean
    BillingIdempotencyStore billingIdempotencyStore() {
      return new InMemoryBillingIdempotencyStore();
    }
  }

  static class InMemoryBillingStorageService implements BillingStorageService {

    private int counter = 0;
    private final java.util.Map<String, byte[]> files = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public StoredFile store(UUID documentId,
                            DocumentFileKind kind,
                            DocumentFileVersion version,
                            byte[] content,
                            String filename,
                            String contentType) {
      counter++;
      String storageKey = "memory/" + kind.name().toLowerCase() + "/" + documentId + "/"
          + version.name().toLowerCase() + "-" + counter + ".pdf";
      String checksum = Integer.toHexString(Arrays.hashCode(content));
      files.put(storageKey, content);
      return new StoredFile(storageKey, checksum);
    }

    @Override
    public byte[] read(String storageKey) {
      byte[] content = files.get(storageKey);
      if (content == null) {
        throw new IllegalArgumentException("Unknown storage key " + storageKey);
      }
      return content;
    }

    @Override
    public org.springframework.core.io.Resource loadAsResource(String storageKey) {
      byte[] content = read(storageKey);
      return new org.springframework.core.io.ByteArrayResource(content) {
        @Override
        public String getFilename() {
          return storageKey.substring(storageKey.lastIndexOf('/') + 1);
        }
      };
    }
  }

  static class TestBillingProvider implements BillingProvider {

    private IssueInvoiceResult nextResult;
    private BillingProviderException nextException;
    private InvoicePayload lastPayload;
    private String lastIdempotencyKey;

    void setNextResult(IssueInvoiceResult result) {
      this.nextResult = result;
      this.nextException = null;
    }

    void setNextException(BillingProviderException exception) {
      this.nextException = exception;
      this.nextResult = null;
    }

    InvoicePayload getLastPayload() {
      return lastPayload;
    }

    String getLastIdempotencyKey() {
      return lastIdempotencyKey;
    }

    @Override
    public IssueInvoiceResult issueInvoice(InvoicePayload payload, String idempotencyKey)
        throws BillingProviderException {
      this.lastPayload = payload;
      this.lastIdempotencyKey = idempotencyKey;
       if (nextException != null) {
         BillingProviderException ex = nextException;
         nextException = null;
         throw ex;
       }
      if (nextResult == null) {
        throw new BillingProviderException("Test provider has no configured response");
      }
      return nextResult;
    }

    @Override
    public ProviderDocument fetchDocument(String providerDocumentId) {
      throw new UnsupportedOperationException("Not implemented in test");
    }
  }

  static class InMemoryBillingIdempotencyStore implements BillingIdempotencyStore {

    private final ConcurrentMap<String, BillingIdempotencyStore.IdempotencyEntry> entries = new ConcurrentHashMap<>();

    @Override
    public Optional<IdempotencyEntry> findEntry(UUID tenantId, String idempotencyKey) {
      return Optional.ofNullable(entries.get(key(tenantId, idempotencyKey)));
    }

    @Override
    public boolean reserve(UUID tenantId, String idempotencyKey, String payloadHash) {
      String key = key(tenantId, idempotencyKey);
      return entries.putIfAbsent(key, new IdempotencyEntry(null, payloadHash)) == null;
    }

    @Override
    public Optional<IdempotencyEntry> awaitCompletion(UUID tenantId, String idempotencyKey) {
      return findEntry(tenantId, idempotencyKey)
          .filter(entry -> entry.documentId() != null);
    }

    @Override
    public void complete(UUID tenantId, String idempotencyKey, String payloadHash, UUID documentId) {
      entries.put(key(tenantId, idempotencyKey), new IdempotencyEntry(documentId, payloadHash));
    }

    @Override
    public void invalidate(UUID tenantId, String idempotencyKey) {
      entries.remove(key(tenantId, idempotencyKey));
    }

    private String key(UUID tenantId, String idempotencyKey) {
      return tenantId + ":" + idempotencyKey;
    }
  }
}
