package com.datakomerz.pymes.billing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record InvoiceSaleDto(
    @NotNull UUID id,
    @NotNull @Valid List<InvoiceSaleItemDto> items,
    @NotNull BigDecimal net,
    @NotNull BigDecimal vat,
    @NotNull BigDecimal total,
    @Size(max = 80) String customerName,
    @Size(max = 30) String customerTaxId,
    @Size(max = 40) String pointOfSale,
    @Size(max = 40) String deviceId
) {
}
