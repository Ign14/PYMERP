package com.datakomerz.pymes.inventory.dto;

import jakarta.validation.constraints.NotBlank;

public record InventoryLocationRequest(
    String code,
    @NotBlank String name,
    String description,
    Boolean enabled
) {}
