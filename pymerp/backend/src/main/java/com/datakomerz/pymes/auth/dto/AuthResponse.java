package com.datakomerz.pymes.auth.dto;

import java.util.Set;
import java.util.UUID;

public record AuthResponse(
  String token,
  long expiresIn,
  String refreshToken,
  long refreshExpiresIn,
  UUID companyId,
  String email,
  String name,
  Set<String> roles,
  Set<String> modules
) {}
