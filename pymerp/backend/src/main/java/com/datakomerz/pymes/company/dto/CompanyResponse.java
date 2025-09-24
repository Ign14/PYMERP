package com.datakomerz.pymes.company.dto;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CompanyResponse(
  UUID id,
  String name,
  String rut,
  String industry,
  LocalTime openTime,
  LocalTime closeTime,
  String receiptFooter,
  String logoUrl,
  OffsetDateTime createdAt
) {}
