package com.datakomerz.pymes.company.dto;

import jakarta.validation.constraints.NotBlank;

public record ParentLocationRequest(
  @NotBlank String name,
  @NotBlank String code
) {}
