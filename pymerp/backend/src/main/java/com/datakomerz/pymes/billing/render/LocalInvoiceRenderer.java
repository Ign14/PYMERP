package com.datakomerz.pymes.billing.render;

import com.company.billing.persistence.FiscalDocument;
import com.company.billing.persistence.NonFiscalDocument;
import com.datakomerz.pymes.billing.dto.PurchaseOrderPayload;
import com.datakomerz.pymes.sales.Sale;

public interface LocalInvoiceRenderer {

  RenderedInvoice renderContingencyFiscalPdf(FiscalDocument document, Sale sale);

  RenderedInvoice renderNonFiscalPdf(NonFiscalDocument document, Sale sale);

  RenderedInvoice renderQuotationPdf(NonFiscalDocument document, Sale sale);

  RenderedInvoice renderDeliveryNotePdf(NonFiscalDocument document, Sale sale);

  RenderedInvoice renderCreditNotePdf(FiscalDocument document, Sale sale);

  RenderedInvoice renderPurchaseOrderPdf(PurchaseOrderPayload payload);

  RenderedInvoice renderReceptionGuidePdf(PurchaseOrderPayload payload);

  record RenderedInvoice(byte[] content, String filename, String contentType) {
  }
}
