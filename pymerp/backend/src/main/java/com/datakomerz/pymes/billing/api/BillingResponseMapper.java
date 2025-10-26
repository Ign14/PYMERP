package com.datakomerz.pymes.billing.api;

import com.datakomerz.pymes.billing.dto.DocumentDetailResponse;
import com.datakomerz.pymes.billing.dto.DocumentFileResponse;
import com.datakomerz.pymes.billing.dto.DocumentLinksResponse;
import com.datakomerz.pymes.billing.dto.InvoiceResponse;
import com.datakomerz.pymes.billing.dto.NonFiscalDocumentResponse;
import com.datakomerz.pymes.billing.service.BillingDocumentView;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class BillingResponseMapper {

  public InvoiceResponse toInvoiceResponse(BillingDocumentView view) {
    return new InvoiceResponse(
        view.id(),
        view.fiscalDocumentType(),
        view.fiscalStatus(),
        view.taxMode(),
        view.number(),
        view.provisionalNumber(),
        view.trackId(),
        view.provider(),
        view.offline(),
        view.createdAt(),
        view.updatedAt(),
        toLinks(view.links()),
        toFileResponses(view.files()));
  }

  public NonFiscalDocumentResponse toNonFiscalResponse(BillingDocumentView view) {
    return new NonFiscalDocumentResponse(
        view.id(),
        view.nonFiscalDocumentType(),
        view.nonFiscalStatus(),
        view.number(),
        view.createdAt(),
        view.updatedAt(),
        toLinks(view.links()),
        toFileResponses(view.files()));
  }

  public DocumentDetailResponse toDetailResponse(BillingDocumentView view) {
    return toDetailResponse(view, toLinks(view.links()));
  }

  public DocumentDetailResponse toDetailResponse(BillingDocumentView view, DocumentLinksResponse links) {
    String status = view.fiscalStatus() != null
        ? view.fiscalStatus().name()
        : view.nonFiscalStatus() != null ? view.nonFiscalStatus().name() : null;
    return new DocumentDetailResponse(
        view.id(),
        view.category(),
        view.fiscalDocumentType(),
        view.nonFiscalDocumentType(),
        status,
        view.taxMode(),
        view.number(),
        view.provisionalNumber(),
        view.provider(),
        view.trackId(),
        view.offline(),
        view.createdAt(),
        view.updatedAt(),
        links,
        toFileResponses(view.files()));
  }

  public DocumentLinksResponse toLinks(BillingDocumentView.DocumentLinks links) {
    if (links == null) {
      return new DocumentLinksResponse(null, null, null);
    }
    return new DocumentLinksResponse(
        links.localPdf(),
        links.officialPdf(),
        links.officialXml());
  }

  public List<DocumentFileResponse> toFileResponses(List<BillingDocumentView.DocumentFileView> files) {
    return files.stream()
        .map(file -> new DocumentFileResponse(
            file.id(),
            file.kind(),
            file.version(),
            file.contentType(),
            file.storageKey(),
            file.checksum(),
            file.createdAt()))
        .collect(Collectors.toList());
  }
}
