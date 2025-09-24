package com.datakomerz.pymes.customers.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CustomerResponse(
  UUID id,
  String name,
  String address,
  BigDecimal lat,
  BigDecimal lng,
  String phone,
  String email,
  String segment
) {}
