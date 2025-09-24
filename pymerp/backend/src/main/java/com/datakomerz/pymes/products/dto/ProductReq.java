package com.datakomerz.pymes.products.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductReq(
  @NotBlank @Size(max = 32) String sku,
  @NotBlank @Size(max = 120) String name,
  @Size(max = 400) String description,
  @Size(max = 64) String category,
  @Size(max = 64) String barcode,
  String imageUrl
) {}
