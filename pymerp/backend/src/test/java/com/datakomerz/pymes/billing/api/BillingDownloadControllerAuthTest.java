package com.datakomerz.pymes.billing.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import com.company.billing.persistence.DocumentFile;
import com.company.billing.persistence.DocumentFileKind;
import com.company.billing.persistence.DocumentFileRepository;
import com.company.billing.persistence.DocumentFileVersion;
import com.datakomerz.pymes.billing.service.BillingStorageService;
import org.mockito.BDDMockito;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;

import java.util.UUID;

/**
 * Authorization tests for BillingDownloadController.
 * Validates RBAC rules for invoice downloads:
 * - GET: All roles (downloads are read-only)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(com.datakomerz.pymes.config.TestJwtDecoderConfig.class)
class BillingDownloadControllerAuthTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private DocumentFileRepository documentFileRepository;

  @MockBean
  private BillingStorageService storageService;

  private static final UUID INVOICE_ID = UUID.randomUUID();

  @Test
  @WithMockUser(roles = "READONLY")
  @DisplayName("GET /api/v1/billing/invoices/{id}/pdf - READONLY can download PDF")
  void testDownloadInvoicePdf_ReadonlyRole_Success() throws Exception {
    documentFileRepository.deleteAll();
    DocumentFile file = new DocumentFile();
    file.setDocumentId(INVOICE_ID);
    file.setKind(DocumentFileKind.FISCAL);
    file.setVersion(DocumentFileVersion.OFFICIAL);
    file.setContentType(MediaType.APPLICATION_PDF_VALUE);
    file.setStorageKey("auth-test/offical.pdf");
    file.setChecksum("checksum");
    documentFileRepository.save(file);

    byte[] pdfBytes = "%PDF-1.4".getBytes(StandardCharsets.ISO_8859_1);
    BDDMockito.given(storageService.loadAsResource("auth-test/offical.pdf"))
        .willReturn(new ByteArrayResource(pdfBytes));

    mockMvc.perform(get("/api/v1/billing/documents/{id}/files/OFFICIAL", INVOICE_ID)
        .header("X-Company-Id", UUID.randomUUID().toString()))
      .andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /api/v1/billing/invoices/{id}/xml - Anonymous gets 401 Unauthorized")
  void testDownloadInvoiceXml_Anonymous_Unauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/billing/documents/{id}/files/OFFICIAL", INVOICE_ID))
      .andExpect(status().isUnauthorized());
  }
}
