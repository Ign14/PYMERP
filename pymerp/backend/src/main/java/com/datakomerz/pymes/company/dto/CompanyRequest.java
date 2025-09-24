package com.datakomerz.pymes.company.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyRequest(
  @NotBlank @Size(max = 100) String name,
  @Size(max = 20) String rut,
  @Size(max = 30) String industry,
  LocalTime openTime,
  LocalTime closeTime,
  @Size(max = 200) String receiptFooter,
  String logoUrl
) {}
