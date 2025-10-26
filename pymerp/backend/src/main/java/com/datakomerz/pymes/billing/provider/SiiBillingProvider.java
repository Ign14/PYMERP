package com.datakomerz.pymes.billing.provider;

import com.datakomerz.pymes.billing.model.InvoicePayload;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class SiiBillingProvider implements BillingProvider {

  private static final DateTimeFormatter FOLIO_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withLocale(Locale.ROOT);

  private final Clock clock;

  public SiiBillingProvider(ObjectProvider<Clock> clockProvider) {
    Clock provided = clockProvider.getIfAvailable();
    this.clock = provided != null ? provided : Clock.systemUTC();
  }

  @Override
  public IssueInvoiceResult issueInvoice(InvoicePayload payload, String idempotencyKey) {
    String providerDocumentId = UUID.nameUUIDFromBytes(
        (idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString())
            .getBytes(StandardCharsets.UTF_8)).toString();
    String trackId = "SII-" + providerDocumentId.substring(0, 8).toUpperCase(Locale.ROOT);
    String folio = buildFolio(payload);
    OfficialDocument officialPdf = buildOfficialPdf(payload, folio, providerDocumentId);
    return new IssueInvoiceResult("SII-STUB", providerDocumentId, trackId, folio, officialPdf);
  }

  @Override
  public ProviderDocument fetchDocument(String providerDocumentId) {
    String safeId = providerDocumentId != null ? providerDocumentId : UUID.randomUUID().toString();
    String trackId = "SII-" + safeId.substring(0, 8).toUpperCase(Locale.ROOT);
    String folio = "F-" + safeId.substring(safeId.length() - 8).toUpperCase(Locale.ROOT);
    OfficialDocument officialPdf = buildOfficialPdf(null, folio, safeId);
    return new ProviderDocument("SII-STUB", safeId, trackId, folio, officialPdf);
  }

  private OfficialDocument buildOfficialPdf(InvoicePayload payload, String folio, String providerDocumentId) {
    StringBuilder builder = new StringBuilder();
    builder.append("Fake PDF for ").append(folio).append(" (stub)\n");
    if (payload != null) {
      builder.append("Sale ID: ").append(payload.saleId()).append('\n');
      builder.append("Tax mode: ").append(payload.taxMode()).append('\n');
      builder.append("Fiscal type: ").append(payload.fiscalDocumentType()).append('\n');
      builder.append("Id: ").append(providerDocumentId).append('\n');
    }
    byte[] content = builder.toString().getBytes(StandardCharsets.UTF_8);
    // FIX: we return bytes with PDF content-type to unblock integration while stub is in place.
    return new OfficialDocument(content, folio + ".pdf", "application/pdf", null);
  }

  private String buildFolio(InvoicePayload payload) {
    String prefix = "FOLIO";
    if (payload != null && payload.fiscalDocumentType() != null) {
      prefix = payload.fiscalDocumentType().name();
    }
    String timestamp = FOLIO_FORMATTER.format(Instant.now(clock));
    return prefix + "-" + timestamp;
  }
}
