package com.datakomerz.pymes.customers.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CustomerRequest(
  @NotBlank @Size(max = 120) String name,
  @Size(max = 500) String address,
  @DecimalMin(value = "-90.0", inclusive = true, message = "Latitude must be >= -90")
  @DecimalMax(value = "90.0", inclusive = true, message = "Latitude must be <= 90")
  BigDecimal lat,
  @DecimalMin(value = "-180.0", inclusive = true, message = "Longitude must be >= -180")
  @DecimalMax(value = "180.0", inclusive = true, message = "Longitude must be <= 180")
  BigDecimal lng,
  @Size(max = 20) String phone,
  @Email @Size(max = 120) String email,
  @Size(max = 64) String segment
) {}
