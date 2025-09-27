package com.datakomerz.pymes.suppliers;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupplierRequest(
  @NotBlank @Size(max = 100) String name,
  @NotBlank @Size(max = 20) String rut,
  @Size(max = 200) String address,
  @Size(max = 120) String commune,
  @Size(max = 120) String businessActivity,
  @Size(max = 30) String phone,
  @Email @Size(max = 120) String email
) {}
