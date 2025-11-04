package com.datakomerz.pymes.customers.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(
  UUID id,
  String name,
  String rut,
  String address,
  BigDecimal lat,
  BigDecimal lng,
  String phone,
  String email,
  String segment,
  String contactPerson,
  String notes,
  Boolean active,
  Instant createdAt,
  Instant updatedAt
) {}
