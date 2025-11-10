package com.datakomerz.pymes.company.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public record CompanyCreateRequest(
  @NotBlank String businessName,
  String fantasyName,
  @NotBlank String rut,
  String logoUrl,
  String businessActivity,
  String address,
  String commune,
  String phone,
  String email,
  String receiptFooterMessage,
  List<ParentLocationRequest> parentLocations
) {}
