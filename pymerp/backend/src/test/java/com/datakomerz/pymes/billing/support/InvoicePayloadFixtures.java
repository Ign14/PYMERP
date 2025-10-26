package com.datakomerz.pymes.billing.support;

import com.datakomerz.pymes.billing.dto.IssueInvoiceRequest;
import com.datakomerz.pymes.billing.model.InvoicePayload;
import com.datakomerz.pymes.billing.service.InvoicePayloadFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;

public final class InvoicePayloadFixtures {

  private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
  private static final InvoicePayloadFactory FACTORY = new InvoicePayloadFactory(MAPPER);

  private InvoicePayloadFixtures() {
  }

  public static InvoicePayload fiscalOffline(UUID saleId) {
    return fromRequest("fixtures/billing/issue_invoice_offline.json", saleId);
  }

  public static InvoicePayload fiscalOnline(UUID saleId) {
    return fromRequest("fixtures/billing/issue_invoice_online.json", saleId);
  }

  public static IssueInvoiceRequest issueInvoiceRequest(String resourcePath, UUID saleId) {
    return issueInvoiceRequest(resourcePath, saleId, null);
  }

  public static IssueInvoiceRequest issueInvoiceRequest(String resourcePath,
                                                        UUID saleId,
                                                        String idempotencyKey) {
    ObjectNode node = FixtureLoader.objectNode(resourcePath);
    replaceSaleId(node, saleId);
    if (idempotencyKey != null) {
      node.put("idempotencyKey", idempotencyKey);
    }
    try {
      return MAPPER.treeToValue(node, IssueInvoiceRequest.class);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Unable to map fixture to IssueInvoiceRequest", ex);
    }
  }

  private static InvoicePayload fromRequest(String resourcePath, UUID saleId) {
    IssueInvoiceRequest request = issueInvoiceRequest(resourcePath, saleId);
    return FACTORY.fromInvoiceRequest(request);
  }

  private static void replaceSaleId(ObjectNode node, UUID saleId) {
    node.with("sale").put("id", saleId.toString());
  }
}
