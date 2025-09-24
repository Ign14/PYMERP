package com.datakomerz.pymes.products.dto;

import jakarta.validation.constraints.NotNull;

public record ProductStatusRequest(
  @NotNull Boolean active
) {}
