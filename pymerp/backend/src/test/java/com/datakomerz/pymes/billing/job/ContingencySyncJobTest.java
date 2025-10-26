package com.datakomerz.pymes.billing.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.billing.persistence.ContingencyQueueItem;
import com.company.billing.persistence.ContingencyQueueItemRepository;
import com.company.billing.persistence.ContingencyQueueStatus;
import com.company.billing.persistence.FiscalDocument;
import com.company.billing.persistence.FiscalDocumentRepository;
import com.company.billing.persistence.FiscalDocumentStatus;
import com.company.billing.persistence.FiscalDocumentType;
import com.company.billing.persistence.TaxMode;
import com.datakomerz.pymes.billing.config.BillingOfflineProperties;
import com.datakomerz.pymes.billing.model.InvoicePayload;
import com.datakomerz.pymes.billing.provider.BillingProvider;
import com.datakomerz.pymes.billing.provider.BillingProvider.IssueInvoiceResult;
import com.datakomerz.pymes.billing.provider.BillingProviderException;
import com.datakomerz.pymes.billing.provider.BillingProviderTransientException;
import com.datakomerz.pymes.sales.Sale;
import com.datakomerz.pymes.sales.SaleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@Import({ContingencySyncJob.class, ContingencySyncJobTest.TestBeans.class})
@TestPropertySource(properties = {
    "billing.offline.enabled=true",
    "billing.offline.retry.maxAttempts=3",
    "billing.offline.retry.backoffMs=1000"
})
class ContingencySyncJobTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Autowired
  private ContingencySyncJob job;

  @Autowired
  private ContingencyQueueItemRepository queueRepository;

  @Autowired
  private FiscalDocumentRepository fiscalDocumentRepository;

  @Autowired
  private SaleRepository saleRepository;

  @Autowired
  private TestBillingProvider billingProvider;

  @Autowired
  private MutableClock clock;

  @Autowired
  private MeterRegistry meterRegistry;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private Sale sale;

  @BeforeEach
  void setUp() {
    billingProvider.reset();
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
    saleRepository.saveAndFlush(sale);
  }

  @Test
  void shouldSyncPendingItemSuccessfully() {
    FiscalDocument document = newFiscalDocument();
    ContingencyQueueItem item = newQueueItem(document, "success-key");

    billingProvider.setNextResult(new IssueInvoiceResult(
        "demo-provider",
        "doc-123",
        "track-555",
        "F001-000123",
        null));

    job.processQueue();

    FiscalDocument reloadedDocument = fiscalDocumentRepository.findById(document.getId()).orElseThrow();

    assertThat(reloadedDocument.getStatus()).isEqualTo(FiscalDocumentStatus.SENT);
    assertThat(reloadedDocument.isOffline()).isFalse();
    assertThat(reloadedDocument.getTrackId()).isEqualTo("track-555");
    assertThat(reloadedDocument.getNumber()).isEqualTo("F001-000123");
    assertThat(reloadedDocument.getSyncAttempts()).isEqualTo(1);

    assertThat(queueRepository.findById(item.getId())).isEmpty();

    assertThat(meterRegistry.find("billing.contingency.latencyMs").summary()).isNotNull();
  }

  @Test
  void shouldRequeueOnTransientFailureAndRespectBackoff() {
    FiscalDocument document = newFiscalDocument();
    ContingencyQueueItem item = newQueueItem(document, "transient-key");

    billingProvider.setNextException(new BillingProviderTransientException("Service unavailable"));

    job.processQueue();

    ContingencyQueueItem afterFirstAttempt = queueRepository.findById(item.getId()).orElseThrow();
    FiscalDocument docAfterFirstAttempt = fiscalDocumentRepository.findById(document.getId()).orElseThrow();

    assertThat(afterFirstAttempt.getStatus()).isEqualTo(ContingencyQueueStatus.OFFLINE_PENDING);
    assertThat(afterFirstAttempt.getSyncAttempts()).isEqualTo(1);
    assertThat(docAfterFirstAttempt.getStatus()).isEqualTo(FiscalDocumentStatus.OFFLINE_PENDING);
    assertThat(docAfterFirstAttempt.getSyncAttempts()).isEqualTo(1);

    // Running again without advancing time should keep attempts unchanged
    billingProvider.setNextResult(new IssueInvoiceResult(
        "demo-provider",
        "doc-999",
        "track-999",
        "F001-000999",
        null));
    job.processQueue();

    ContingencyQueueItem stillPending = queueRepository.findById(item.getId()).orElseThrow();
    assertThat(stillPending.getSyncAttempts()).isEqualTo(1);
    assertThat(stillPending.getStatus()).isEqualTo(ContingencyQueueStatus.OFFLINE_PENDING);

    // Advance past backoff window and run again
    clock.advance(Duration.ofSeconds(2));
    job.processQueue();

    FiscalDocument syncedDocument = fiscalDocumentRepository.findById(document.getId()).orElseThrow();

    assertThat(syncedDocument.getStatus()).isEqualTo(FiscalDocumentStatus.SENT);
    assertThat(syncedDocument.getSyncAttempts()).isEqualTo(2);
    assertThat(queueRepository.findById(item.getId())).isEmpty();
  }

  @Test
  void shouldMarkPermanentFailure() {
    FiscalDocument document = newFiscalDocument();
    ContingencyQueueItem item = newQueueItem(document, "permanent-key");

    billingProvider.setNextException(new BillingProviderException("Rule violation"));

    job.processQueue();

    ContingencyQueueItem failedItem = queueRepository.findById(item.getId()).orElseThrow();
    FiscalDocument failedDoc = fiscalDocumentRepository.findById(document.getId()).orElseThrow();
    Counter failures = meterRegistry.find("billing.contingency.failures").counter();

    assertThat(failedItem.getStatus()).isEqualTo(ContingencyQueueStatus.FAILED);
    assertThat(failedDoc.getStatus()).isEqualTo(FiscalDocumentStatus.FAILED);
    assertThat(failedItem.getErrorDetail()).contains("Rule violation");
    assertThat(failedDoc.getErrorDetail()).contains("Rule violation");
    assertThat(failures.count()).isEqualTo(1);
  }

  private FiscalDocument newFiscalDocument() {
    FiscalDocument document = new FiscalDocument();
    document.setSale(sale);
    document.setDocumentType(FiscalDocumentType.FACTURA);
    document.setTaxMode(TaxMode.AFECTA);
    document.setStatus(FiscalDocumentStatus.OFFLINE_PENDING);
    document.setOffline(true);
    document.setProvisionalNumber("CTG-2025-00001");
    document.setIdempotencyKey(UUID.randomUUID().toString());
    fiscalDocumentRepository.saveAndFlush(document);
    return document;
  }

  private ContingencyQueueItem newQueueItem(FiscalDocument document, String keySuffix) {
    ContingencyQueueItem item = new ContingencyQueueItem();
    item.setDocument(document);
    item.setIdempotencyKey("idem-" + keySuffix);
    item.setStatus(ContingencyQueueStatus.OFFLINE_PENDING);
    item.setProviderPayload(newPayloadNode(document));
    queueRepository.saveAndFlush(item);
    return queueRepository.findById(item.getId()).orElseThrow();
  }

  private ObjectNode newPayloadNode(FiscalDocument document) {
    ObjectNode node = MAPPER.createObjectNode();
    node.put("documentId", document.getId().toString());
    node.put("provisionalNumber", document.getProvisionalNumber());
    node.put("generatedAt", Instant.now(clock).toString());
    ObjectNode saleNode = node.putObject("sale");
    saleNode.put("id", sale.getId().toString());
    saleNode.put("deviceId", "dev-1");
    saleNode.put("pointOfSale", "POS-1");
    return node;
  }

  @TestConfiguration
  @EnableConfigurationProperties(BillingOfflineProperties.class)
  static class TestBeans {

    @Bean
    MutableClock testClock() {
      return new MutableClock();
    }

    @Bean
    @Primary
    MeterRegistry meterRegistry() {
      return new SimpleMeterRegistry();
    }

    @Bean
    TestBillingProvider testBillingProvider() {
      return new TestBillingProvider();
    }
  }

  static class TestBillingProvider implements BillingProvider {

    private IssueInvoiceResult nextResult;
    private RuntimeException nextException;

    void setNextResult(IssueInvoiceResult result) {
      this.nextResult = result;
      this.nextException = null;
    }

    void setNextException(RuntimeException exception) {
      this.nextException = exception;
      this.nextResult = null;
    }

    void reset() {
      this.nextResult = null;
      this.nextException = null;
    }

    @Override
    public IssueInvoiceResult issueInvoice(InvoicePayload payload, String idempotencyKey) {
      if (nextException != null) {
        RuntimeException throwable = nextException;
        nextException = null;
        throw throwable;
      }
      if (nextResult == null) {
        throw new BillingProviderException("No configured response for test");
      }
      IssueInvoiceResult result = nextResult;
      nextResult = null;
      return result;
    }

    @Override
    public ProviderDocument fetchDocument(String providerDocumentId) {
      throw new UnsupportedOperationException("Not needed for tests");
    }
  }

  static class MutableClock extends Clock {

    private Instant current = Instant.parse("2025-01-01T00:00:00Z");
    private final ZoneId zone = ZoneId.of("UTC");

    @Override
    public ZoneId getZone() {
      return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return current;
    }

    void advance(Duration duration) {
      current = current.plus(duration);
    }
  }
}
