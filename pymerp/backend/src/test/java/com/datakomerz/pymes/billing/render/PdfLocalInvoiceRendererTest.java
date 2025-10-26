package com.datakomerz.pymes.billing.render;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.company.billing.persistence.FiscalDocument;
import com.company.billing.persistence.FiscalDocumentType;
import com.company.billing.persistence.NonFiscalDocument;
import com.company.billing.persistence.NonFiscalDocumentType;
import com.company.billing.persistence.TaxMode;
import com.datakomerz.pymes.company.Company;
import com.datakomerz.pymes.company.CompanyRepository;
import com.datakomerz.pymes.customers.Customer;
import com.datakomerz.pymes.customers.CustomerRepository;
import com.datakomerz.pymes.products.Product;
import com.datakomerz.pymes.products.ProductRepository;
import com.datakomerz.pymes.sales.Sale;
import com.datakomerz.pymes.sales.SaleItem;
import com.datakomerz.pymes.sales.SaleItemRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.DefaultResourceLoader;

class PdfLocalInvoiceRendererTest {

private static final String EXPECTED_CONTINGENCY_HASH = "df9cb869a1ba3f7511c9daff4e784fb360a47c5d0a29f3d914685c305c53042d";
private static final int EXPECTED_CONTINGENCY_LENGTH = 7666;
private static final String EXPECTED_NON_FISCAL_HASH = "8e854a7bf39044bad947d8ca12be3e058281f3aee5234cebdd9485707f47c7ac";
private static final int EXPECTED_NON_FISCAL_LENGTH = 7914;

  private final SaleItemRepository saleItemRepository = Mockito.mock(SaleItemRepository.class);
  private final ProductRepository productRepository = Mockito.mock(ProductRepository.class);
  private final CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
  private final CompanyRepository companyRepository = Mockito.mock(CompanyRepository.class);
  private final BillingPdfProperties pdfProperties = new BillingPdfProperties();
  private final DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
  private final Clock clock = Clock.fixed(Instant.parse("2025-01-01T12:00:00Z"), ZoneOffset.UTC);
  private PdfLocalInvoiceRenderer renderer;

  private Sale sale;
  private Company company;
  private Customer customer;
  private SaleItem item;
  private Product product;

  @BeforeEach
  void setUp() {
    ObjectProvider<Clock> clockProvider = new ObjectProvider<>() {
      @Override
      public Clock getObject(Object... args) {
        return clock;
      }

      @Override
      public Clock getIfAvailable() {
        return clock;
      }

      @Override
      public Clock getIfUnique() {
        return clock;
      }

      @Override
      public Clock getObject() {
        return clock;
      }
    };

    pdfProperties.setDocumentsBaseUrl("https://demo.local/api/v1");
    pdfProperties.getBranding().setLogoPath(null);

    renderer = new PdfLocalInvoiceRenderer(
        saleItemRepository,
        productRepository,
        customerRepository,
        companyRepository,
        pdfProperties,
        resourceLoader,
        clockProvider,
        "Documento emitido en modo contingencia. Sera validado con el SII.");

    UUID companyId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    UUID customerId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
    UUID saleId = UUID.fromString("cccccccc-0000-0000-0000-000000000001");

    sale = new Sale();
    sale.setId(saleId);
    sale.setCompanyId(companyId);
    sale.setCustomerId(customerId);
    sale.setPaymentMethod("TRANSFERENCIA");
    sale.setDocType("VENTA");
    sale.setIssuedAt(OffsetDateTime.of(2024, 12, 31, 18, 30, 0, 0, ZoneOffset.UTC));
    sale.setNet(new BigDecimal("15000.00"));
    sale.setVat(new BigDecimal("2850.00"));
    sale.setTotal(new BigDecimal("17850.00"));

    company = new Company();
    company.setId(companyId);
    company.setBusinessName("Demo SpA");
    company.setRut("76.123.456-7");
    company.setBusinessActivity("Servicios TI");
    company.setAddress("Av. Demo 123");
    company.setCommune("Santiago");
    company.setPhone("+56 2 2222 2222");
    company.setEmail("contacto@demo.cl");
    company.setReceiptFooterMessage("Gracias por preferir Demo SpA.");

    customer = new Customer();
    customer.setId(customerId);
    customer.setCompanyId(companyId);
    customer.setName("Cliente Demo");
    customer.setAddress("Calle Cliente 456");
    customer.setPhone("+56 9 9999 9999");
    customer.setEmail("cliente@example.com");

    item = new SaleItem();
    item.setSaleId(saleId);
    item.setProductId(UUID.fromString("dddddddd-0000-0000-0000-000000000001"));
    item.setQty(new BigDecimal("2"));
    item.setUnitPrice(new BigDecimal("7500.00"));
    item.setDiscount(BigDecimal.ZERO);

    product = new Product();
    product.setId(item.getProductId());
    product.setCompanyId(companyId);
    product.setName("Servicio Premium");
    product.setSku("SRV-001");

    when(saleItemRepository.findBySaleId(saleId)).thenReturn(List.of(item));
    when(productRepository.findById(item.getProductId())).thenReturn(Optional.of(product));
    when(customerRepository.findByIdAndCompanyId(customerId, companyId)).thenReturn(Optional.of(customer));
    when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
  }

  @Test
  void renderContingencyFiscalPdf_generatesDeterministicOutput() {
    FiscalDocument document = new FiscalDocument();
    document.setId(UUID.fromString("eeeeeeee-0000-0000-0000-000000000001"));
    document.setSale(sale);
    document.setDocumentType(FiscalDocumentType.FACTURA);
    document.setTaxMode(TaxMode.AFECTA);
    document.setProvisionalNumber("CTG-2025-00001");

    LocalInvoiceRenderer.RenderedInvoice invoice = renderer.renderContingencyFiscalPdf(document, sale);

    assertThat(invoice.contentType()).isEqualTo("application/pdf");
    assertThat(invoice.filename()).isEqualTo("contingencia-CTG-2025-00001.pdf");
    assertThat(invoice.content().length).isGreaterThan(1024);
    String text = extractText(invoice.content());
    String digest = sha256(text.getBytes(StandardCharsets.UTF_8));
    assertThat(digest).isEqualTo(EXPECTED_CONTINGENCY_HASH);
    assertThat(invoice.content().length).isEqualTo(EXPECTED_CONTINGENCY_LENGTH);
    assertThat(text).contains("Contingencia", document.getProvisionalNumber());
  }

  @Test
  void renderNonFiscalPdf_generatesDeterministicOutput() {
    NonFiscalDocument document = new NonFiscalDocument();
    document.setId(UUID.fromString("ffffffff-0000-0000-0000-000000000001"));
    document.setSale(sale);
    document.setDocumentType(NonFiscalDocumentType.COTIZACION);
    document.setNumber("NF-2025-00042");

    LocalInvoiceRenderer.RenderedInvoice invoice = renderer.renderNonFiscalPdf(document, sale);

    assertThat(invoice.contentType()).isEqualTo("application/pdf");
    assertThat(invoice.filename()).isEqualTo("cotizacion-NF-2025-00042.pdf");
    assertThat(invoice.content().length).isGreaterThan(1024);
    String text = extractText(invoice.content());
    String digest = sha256(text.getBytes(StandardCharsets.UTF_8));
    assertThat(digest).isEqualTo(EXPECTED_NON_FISCAL_HASH);
    assertThat(invoice.content().length).isEqualTo(EXPECTED_NON_FISCAL_LENGTH);
    assertThat(text).contains("Documento", document.getNumber());
  }

  private String extractText(byte[] pdfBytes) {
    try (PDDocument document = PDDocument.load(pdfBytes)) {
      PDFTextStripper stripper = new PDFTextStripper();
      return stripper.getText(document).replace("\r\n", "\n").trim();
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to extract text from rendered PDF", ex);
    }
  }

  private static String sha256(byte[] content) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(content));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("Missing SHA-256 digest", ex);
    }
  }
}
