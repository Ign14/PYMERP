package com.datakomerz.pymes.company.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CompanyResponse(
  UUID id,
  String businessName,
  String rut,
  String businessActivity,
  String address,
  String commune,
  String phone,
  String email,
  String receiptFooterMessage,
  OffsetDateTime createdAt,
  OffsetDateTime updatedAt
) {}
