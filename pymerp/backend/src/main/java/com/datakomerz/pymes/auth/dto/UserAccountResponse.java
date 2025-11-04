package com.datakomerz.pymes.auth.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record UserAccountResponse(
  UUID id,
  String email,
  String name,
  String role,
  String status,
  List<String> roles,
  OffsetDateTime createdAt
) {}
