package com.datakomerz.pymes.billing.api;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.billing.persistence.DocumentFileKind;
import com.company.billing.persistence.DocumentFileVersion;
import com.company.billing.persistence.FiscalDocumentStatus;
import com.company.billing.persistence.FiscalDocumentType;
import com.company.billing.persistence.NonFiscalDocumentStatus;
import com.company.billing.persistence.NonFiscalDocumentType;
import com.company.billing.persistence.TaxMode;
import com.datakomerz.pymes.billing.dto.IssueInvoiceRequest;
import com.datakomerz.pymes.billing.model.DocumentCategory;
import com.datakomerz.pymes.billing.model.InvoicePayload;
import com.datakomerz.pymes.billing.service.BillingDocumentView;
import com.datakomerz.pymes.billing.service.BillingDocumentView.DocumentFileView;
import com.datakomerz.pymes.billing.service.BillingDocumentView.DocumentLinks;
import com.datakomerz.pymes.billing.service.BillingService;
import com.datakomerz.pymes.billing.service.InvoicePayloadFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class BillingControllerTest {

  @InjectMocks
  private BillingController billingController;

  @Mock
  private BillingService billingService;

  @Mock
  private InvoicePayloadFactory payloadFactory;

  private MockMvc mockMvc;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc = MockMvcBuilders.standaloneSetup(billingController).build();
  }

  @Test
  void issueInvoice_returnsCreatedResponseWithLinks() throws Exception {
    UUID saleId = UUID.randomUUID();
    InvoicePayload payload = new InvoicePayload(
        saleId,
        FiscalDocumentType.FACTURA,
        null,
        TaxMode.AFECTA,
        "device-1",
        "POS-1",
        objectMapper.createObjectNode());

    when(payloadFactory.fromInvoiceRequest(any(IssueInvoiceRequest.class))).thenReturn(payload);

    BillingDocumentView view = new BillingDocumentView(
        DocumentCategory.FISCAL,
        UUID.randomUUID(),
        FiscalDocumentType.FACTURA,
        null,
        FiscalDocumentStatus.OFFLINE_PENDING,
        null,
        TaxMode.AFECTA,
        null,
        "CTG-2025-00001",
        null,
        null,
        true,
        OffsetDateTime.now(ZoneOffset.UTC),
        OffsetDateTime.now(ZoneOffset.UTC),
        new DocumentLinks("billing/fiscal/local.pdf", null),
        List.of(new DocumentFileView(
            UUID.randomUUID(),
            DocumentFileKind.FISCAL,
            DocumentFileVersion.LOCAL,
            "application/pdf",
            "billing/fiscal/local.pdf",
            "abc123",
            OffsetDateTime.now(ZoneOffset.UTC)
        )));

    when(billingService.issueInvoice(anyBoolean(), any(), eq(payload), eq("invoice-req-key")))
        .thenReturn(view);

    Map<String, Object> request = buildInvoiceRequestPayload(saleId);

    mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/billing/invoices")
            .header("Idempotency-Key", "invoice-req-key")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(MockMvcResultMatchers.status().isCreated())
        .andExpect(MockMvcResultMatchers.jsonPath("$.documentType", is("FACTURA")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.status", is("OFFLINE_PENDING")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.links.localPdf", is("billing/fiscal/local.pdf")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.links.officialPdf").doesNotExist())
        .andExpect(MockMvcResultMatchers.jsonPath("$.files[0].storageKey", is("billing/fiscal/local.pdf")));

    verify(billingService).issueInvoice(anyBoolean(), any(), eq(payload), eq("invoice-req-key"));
  }

  @Test
  void issueInvoice_whenIdempotencyMismatch_returnsBadRequest() throws Exception {
    Map<String, Object> request = new HashMap<>(buildInvoiceRequestPayload(UUID.randomUUID()));
    request.put("idempotencyKey", "body-key");

    mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/billing/invoices")
            .header("Idempotency-Key", "header-key")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(MockMvcResultMatchers.status().isBadRequest());

    verify(billingService, never()).issueInvoice(anyBoolean(), any(), any(), any());
  }

  @Test
  void createNonFiscal_returnsReadyDocument() throws Exception {
    UUID saleId = UUID.randomUUID();
    InvoicePayload payload = new InvoicePayload(
        saleId,
        null,
        NonFiscalDocumentType.COTIZACION,
        null,
        "device-3",
        "POS-3",
        objectMapper.createObjectNode());
    when(payloadFactory.fromNonFiscalRequest(any())).thenReturn(payload);

    BillingDocumentView view = new BillingDocumentView(
        DocumentCategory.NON_FISCAL,
        UUID.randomUUID(),
        null,
        NonFiscalDocumentType.COTIZACION,
        null,
        NonFiscalDocumentStatus.READY,
        null,
        "NF-2025-00001",
        null,
        null,
        null,
        false,
        OffsetDateTime.now(ZoneOffset.UTC),
        OffsetDateTime.now(ZoneOffset.UTC),
        new DocumentLinks("billing/non-fiscal/local.pdf", null),
        List.of(new DocumentFileView(
            UUID.randomUUID(),
            DocumentFileKind.NON_FISCAL,
            DocumentFileVersion.LOCAL,
            "application/pdf",
            "billing/non-fiscal/local.pdf",
            "xyz999",
            OffsetDateTime.now(ZoneOffset.UTC)
        )));

    when(billingService.createNonFiscal(payload)).thenReturn(view);

    Map<String, Object> request = buildNonFiscalRequestPayload(saleId);

    mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/billing/non-fiscal")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(MockMvcResultMatchers.status().isCreated())
        .andExpect(MockMvcResultMatchers.jsonPath("$.documentType", is("COTIZACION")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.status", is("READY")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.links.localPdf", notNullValue()));
  }

  @Test
  void getDocument_returnsDetailResponse() throws Exception {
    UUID documentId = UUID.randomUUID();
    BillingDocumentView view = new BillingDocumentView(
        DocumentCategory.FISCAL,
        documentId,
        FiscalDocumentType.BOLETA,
        null,
        FiscalDocumentStatus.SENT,
        null,
        TaxMode.EXENTA,
        "B-100",
        "CTG-2025-00002",
        "tax-provider",
        "track-55",
        false,
        OffsetDateTime.now(ZoneOffset.UTC),
        OffsetDateTime.now(ZoneOffset.UTC),
        new DocumentLinks("billing/fiscal/local.pdf", "billing/fiscal/official.pdf"),
        List.of(
            new DocumentFileView(
                UUID.randomUUID(),
                DocumentFileKind.FISCAL,
                DocumentFileVersion.LOCAL,
                "application/pdf",
                "billing/fiscal/local.pdf",
                "aaa",
                OffsetDateTime.now(ZoneOffset.UTC)
            ),
            new DocumentFileView(
                UUID.randomUUID(),
                DocumentFileKind.FISCAL,
                DocumentFileVersion.OFFICIAL,
                "application/pdf",
                "billing/fiscal/official.pdf",
                "bbb",
                OffsetDateTime.now(ZoneOffset.UTC)
            )));

    when(billingService.getDocument(documentId)).thenReturn(view);

    mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/billing/documents/{id}", documentId))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.category", is("FISCAL")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.provisionalNumber", is("CTG-2025-00002")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.links.officialPdf", is("billing/fiscal/official.pdf")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.files[1].version", is("OFFICIAL")));
  }

  private Map<String, Object> buildInvoiceRequestPayload(UUID saleId) {
    return Map.of(
        "documentType", "FACTURA",
        "taxMode", "AFECTA",
        "idempotencyKey", "invoice-req-key",
        "forceOffline", false,
        "connectivityHint", "GOOD",
        "sale", Map.of(
            "id", saleId.toString(),
            "items", List.of(Map.of(
                "description", "Producto demo",
                "quantity", BigDecimal.ONE,
                "unitPrice", new BigDecimal("10000"),
                "discount", BigDecimal.ZERO
            )),
            "net", new BigDecimal("10000"),
            "vat", new BigDecimal("1900"),
            "total", new BigDecimal("11900"),
            "customerName", "Cliente Demo",
            "customerTaxId", "12345678-9",
            "pointOfSale", "POS-1",
            "deviceId", "device-1"
        )
    );
  }

  private Map<String, Object> buildNonFiscalRequestPayload(UUID saleId) {
    return Map.of(
        "documentType", "COTIZACION",
        "sale", Map.of(
            "id", saleId.toString(),
            "items", List.of(),
            "net", new BigDecimal("10000"),
            "vat", new BigDecimal("0"),
            "total", new BigDecimal("10000"),
            "pointOfSale", "POS-4",
            "deviceId", "device-4"
        )
    );
  }
}
