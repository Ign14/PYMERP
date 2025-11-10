package com.datakomerz.pymes.company.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CompanyResponse(
  UUID id,
  String businessName,
  String fantasyName,
  String rut,
  String logoUrl,
  String businessActivity,
  String address,
  String commune,
  String phone,
  String email,
  String receiptFooterMessage,
  List<ParentLocationResponse> parentLocations,
  OffsetDateTime createdAt,
  OffsetDateTime updatedAt
) {}
