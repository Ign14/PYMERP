package com.datakomerz.pymes.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserPasswordRequest(
  @NotBlank @Size(min = 8, max = 72) String password
) {}
