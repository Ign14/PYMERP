package com.datakomerz.pymes.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
  @NotBlank String refreshToken
) {}
