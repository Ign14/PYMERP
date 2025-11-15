package com.datakomerz.pymes.inventory.dto;

public record InventoryLocationPatchRequest(
    String code,
    String name,
    String description,
    Boolean enabled
) {}
