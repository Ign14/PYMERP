package com.datakomerz.pymes.billing.service;

import com.company.billing.persistence.SiiDocumentType;
import com.datakomerz.pymes.billing.dto.IssueInvoiceRequest;
import com.datakomerz.pymes.billing.dto.NonFiscalDocumentRequest;
import com.datakomerz.pymes.billing.model.InvoicePayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

@Component
public class InvoicePayloadFactory {

  private final ObjectMapper objectMapper;

  public InvoicePayloadFactory(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public InvoicePayload fromInvoiceRequest(IssueInvoiceRequest request) {
    ObjectNode payload = objectMapper.valueToTree(request);
    SiiDocumentType siiType = SiiDocumentType.from(request.documentType(), request.taxMode());
    return new InvoicePayload(
        request.sale().id(),
        request.documentType(),
        null,
        request.taxMode(),
        siiType,
        request.sale().deviceId(),
        request.sale().pointOfSale(),
        payload);
  }

  public InvoicePayload fromNonFiscalRequest(NonFiscalDocumentRequest request) {
    ObjectNode payload = objectMapper.valueToTree(request);
    return new InvoicePayload(
        request.sale().id(),
        null,
        request.documentType(),
        null,
        null,
        request.sale().deviceId(),
        request.sale().pointOfSale(),
        payload);
  }
}
