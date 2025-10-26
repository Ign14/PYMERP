package com.datakomerz.pymes.billing.render;

import com.company.billing.persistence.FiscalDocument;
import com.company.billing.persistence.NonFiscalDocument;
import com.datakomerz.pymes.sales.Sale;

public interface LocalInvoiceRenderer {

  RenderedInvoice renderContingencyFiscalPdf(FiscalDocument document, Sale sale);

  RenderedInvoice renderNonFiscalPdf(NonFiscalDocument document, Sale sale);

  record RenderedInvoice(byte[] content, String filename, String contentType) {
  }
}
