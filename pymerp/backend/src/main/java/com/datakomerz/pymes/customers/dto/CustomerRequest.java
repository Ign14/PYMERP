package com.datakomerz.pymes.customers.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CustomerRequest(
  @NotBlank @Size(max = 120) String name,
  @Size(max = 20)
  @Pattern(
    regexp = "^(?:\\d{1,2}\\.\\d{3}\\.\\d{3}-[0-9kK]|\\d{7,8}-[0-9kK])?$",
    message = "RUT debe tener formato XX.XXX.XXX-X"
  )
  String rut,
  @Size(max = 500) String address,
  @DecimalMin(value = "-90.0", inclusive = true, message = "Latitude must be >= -90")
  @DecimalMax(value = "90.0", inclusive = true, message = "Latitude must be <= 90")
  BigDecimal lat,
  @DecimalMin(value = "-180.0", inclusive = true, message = "Longitude must be >= -180")
  @DecimalMax(value = "180.0", inclusive = true, message = "Longitude must be <= 180")
  BigDecimal lng,
  @Size(max = 20) 
  @Pattern(regexp = "^\\+?[0-9\\s-()]*$|^$", 
           message = "Teléfono contiene caracteres inválidos")
  String phone,
  @Email @Size(max = 120) String email,
  @Size(max = 64) String segment,
  @Size(max = 120) String contactPerson,
  String notes,
  Boolean active
) {}
