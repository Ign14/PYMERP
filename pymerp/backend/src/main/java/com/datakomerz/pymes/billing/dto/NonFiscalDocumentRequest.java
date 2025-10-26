package com.datakomerz.pymes.billing.dto;

import com.company.billing.persistence.NonFiscalDocumentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NonFiscalDocumentRequest(
    @NotNull NonFiscalDocumentType documentType,
    @NotNull @Valid InvoiceSaleDto sale,
    @Size(max = 120) String title,
    @Size(max = 240) String notes
) {
}
