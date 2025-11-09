package com.company.billing.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.datakomerz.pymes.sales.Sale;
import com.datakomerz.pymes.sales.SaleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@EntityScan(basePackages = {"com.datakomerz.pymes", "com.company.billing.persistence"})
@EnableJpaRepositories(basePackages = {"com.datakomerz.pymes", "com.company.billing.persistence"})
@Import(BillingPersistenceRepositoryTest.TestConfig.class)
class BillingPersistenceRepositoryTest {

  @TestConfiguration
  static class TestConfig {
    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper();
    }
  }

  @SpringBootConfiguration
  static class BillingPersistenceTestApplication {
  }

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private FiscalDocumentRepository fiscalDocumentRepository;

  @Autowired
  private NonFiscalDocumentRepository nonFiscalDocumentRepository;

  @Autowired
  private DocumentFileRepository documentFileRepository;

  @Autowired
  private ContingencyQueueItemRepository contingencyQueueItemRepository;

  @Autowired
  private SaleRepository saleRepository;

  @Autowired
  private ObjectMapper objectMapper;

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
    sale.setNet(BigDecimal.valueOf(10000));
    sale.setVat(BigDecimal.valueOf(1900));
    sale.setTotal(BigDecimal.valueOf(11900));
    sale.setPaymentMethod("EFECTIVO");
    sale.setDocType("FACTURA");
    sale.setIssuedAt(OffsetDateTime.now());
    sale.setPaymentTermDays(30); // Required field after V29 migration
    sale = saleRepository.saveAndFlush(sale);
  }

  @Test
  void shouldPersistAndQueryFiscalDocumentGraph() {
    FiscalDocument fiscalDocument = new FiscalDocument();
    fiscalDocument.setSale(sale);
    fiscalDocument.setDocumentType(FiscalDocumentType.FACTURA);
    fiscalDocument.setTaxMode(TaxMode.AFECTA);
    fiscalDocument.setStatus(FiscalDocumentStatus.OFFLINE_PENDING);
    fiscalDocument.setProvisionalNumber("CTG-2025-0001");
    fiscalDocument.setProvider("demo-provider");
    fiscalDocument.setTrackId("track-123");
    fiscalDocument.setOffline(true);

    FiscalDocument saved = fiscalDocumentRepository.save(fiscalDocument);
    entityManager.flush();

    List<FiscalDocument> bySale = fiscalDocumentRepository.findBySale_Id(sale.getId());
    assertThat(bySale).hasSize(1).first().extracting(FiscalDocument::getProvisionalNumber)
        .isEqualTo("CTG-2025-0001");

    assertThat(fiscalDocumentRepository.findByProvisionalNumber("CTG-2025-0001"))
        .isPresent();

    saved.setStatus(FiscalDocumentStatus.SYNCING);
    fiscalDocumentRepository.save(saved);

    assertThat(fiscalDocumentRepository.findByStatus(FiscalDocumentStatus.SYNCING))
        .extracting(FiscalDocument::getProvider)
        .containsExactly("demo-provider");
  }

  @Test
  void shouldPersistNonFiscalDocumentAndFiles() {
    NonFiscalDocument nonFiscalDocument = new NonFiscalDocument();
    nonFiscalDocument.setSale(sale);
    nonFiscalDocument.setDocumentType(NonFiscalDocumentType.COTIZACION);
    nonFiscalDocument.setNumber("NF-001");
    nonFiscalDocumentRepository.saveAndFlush(nonFiscalDocument);

    DocumentFile localFile = new DocumentFile();
    localFile.setDocumentId(nonFiscalDocument.getId());
    localFile.setKind(DocumentFileKind.NON_FISCAL);
    localFile.setVersion(DocumentFileVersion.LOCAL);
    localFile.setContentType("application/pdf");
    localFile.setStorageKey("local/nf-001.pdf");
    documentFileRepository.saveAndFlush(localFile);

    DocumentFile officialFile = new DocumentFile();
    officialFile.setDocumentId(nonFiscalDocument.getId());
    officialFile.setKind(DocumentFileKind.NON_FISCAL);
    officialFile.setVersion(DocumentFileVersion.OFFICIAL);
    officialFile.setContentType("application/pdf");
    officialFile.setStorageKey("official/nf-001.pdf");
    officialFile.setPreviousFile(localFile);
    documentFileRepository.saveAndFlush(officialFile);

    List<DocumentFile> files = documentFileRepository.findByDocumentIdOrderByCreatedAtAsc(
        nonFiscalDocument.getId());
    assertThat(files).hasSize(2);
    assertThat(documentFileRepository
        .findByDocumentIdAndVersion(nonFiscalDocument.getId(), DocumentFileVersion.OFFICIAL))
        .singleElement()
        .extracting(DocumentFile::getPreviousFile)
        .isEqualTo(localFile);
  }

  @Test
  void shouldPersistContingencyQueueItem() {
    FiscalDocument fiscalDocument = new FiscalDocument();
    fiscalDocument.setSale(sale);
    fiscalDocument.setDocumentType(FiscalDocumentType.BOLETA);
    fiscalDocument.setTaxMode(TaxMode.EXENTA);
    fiscalDocument.setStatus(FiscalDocumentStatus.OFFLINE_PENDING);
    fiscalDocument.setProvisionalNumber("CTG-2025-0099");
    fiscalDocumentRepository.saveAndFlush(fiscalDocument);

    ObjectNode payload = objectMapper.createObjectNode()
        .put("documentId", fiscalDocument.getId().toString())
        .put("total", 9900);

    ContingencyQueueItem queueItem = new ContingencyQueueItem();
    queueItem.setDocument(fiscalDocument);
    queueItem.setIdempotencyKey("sale-" + fiscalDocument.getId());
    queueItem.setProviderPayload(payload);
    queueItem.setStatus(ContingencyQueueStatus.OFFLINE_PENDING);
    contingencyQueueItemRepository.save(queueItem);

    List<ContingencyQueueItem> pending = contingencyQueueItemRepository
        .findByStatusOrderByCreatedAtAsc(ContingencyQueueStatus.OFFLINE_PENDING);
    assertThat(pending).hasSize(1);

    assertThat(contingencyQueueItemRepository
        .findByIdempotencyKey("sale-" + fiscalDocument.getId())).isPresent();
  }
}
