package com.datakomerz.pymes.suppliers;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupplierRequest(
  @NotBlank @Size(max = 100) String name,
  @Size(max = 20) String rut
) {}
