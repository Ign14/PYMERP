package com.datakomerz.pymes.company.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.datakomerz.pymes.common.validation.Rut;

public record CompanyRequest(
  @NotBlank @Size(max = 160) String businessName,
  @NotBlank @Rut String rut,
  @Size(max = 120) String businessActivity,
  @Size(max = 160) String address,
  @Size(max = 80) String commune,
  @Size(max = 40) String phone,
  @Email @Size(max = 160) String email,
  @Size(max = 500) String receiptFooterMessage
) {}
