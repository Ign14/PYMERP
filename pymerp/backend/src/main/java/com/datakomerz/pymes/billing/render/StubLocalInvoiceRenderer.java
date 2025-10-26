package com.datakomerz.pymes.billing.render;

import com.company.billing.persistence.FiscalDocument;
import com.company.billing.persistence.NonFiscalDocument;
import com.datakomerz.pymes.sales.Sale;
import java.nio.charset.StandardCharsets;

public class StubLocalInvoiceRenderer implements LocalInvoiceRenderer {

  @Override
  public RenderedInvoice renderContingencyFiscalPdf(FiscalDocument document, Sale sale) {
    String identifier = document != null && document.getProvisionalNumber() != null
        ? document.getProvisionalNumber() : (sale != null && sale.getId() != null ? sale.getId().toString() : "fiscal");
    byte[] content = ("PDF placeholder for fiscal " + identifier).getBytes(StandardCharsets.UTF_8);
    String filename = "fiscal-" + identifier + ".pdf";
    return new RenderedInvoice(content, filename, "application/pdf");
  }

  @Override
  public RenderedInvoice renderNonFiscalPdf(NonFiscalDocument document, Sale sale) {
    String identifier = document != null && document.getNumber() != null
        ? document.getNumber() : (sale != null && sale.getId() != null ? sale.getId().toString() : "non-fiscal");
    byte[] content = ("PDF placeholder for non fiscal " + identifier).getBytes(StandardCharsets.UTF_8);
    String filename = "non-fiscal-" + identifier + ".pdf";
    return new RenderedInvoice(content, filename, "application/pdf");
  }
}
