package com.datakomerz.pymes.billing.service;

import com.datakomerz.pymes.billing.dto.IssueInvoiceRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyPayloadHasher {

  private final JsonMapper mapper;
  private final HexFormat hexFormat;

  public IdempotencyPayloadHasher() {
    this.mapper = JsonMapper.builder()
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
        .build();
    this.hexFormat = HexFormat.of();
  }

  public String hash(IssueInvoiceRequest request) {
    try {
      byte[] normalized = mapper.writeValueAsBytes(request);
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(normalized);
      return hexFormat.formatHex(hashed);
    } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
      throw new IllegalStateException("Unable to hash invoice request", ex);
    }
  }
}
