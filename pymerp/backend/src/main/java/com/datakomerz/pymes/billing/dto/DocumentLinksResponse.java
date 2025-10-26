package com.datakomerz.pymes.billing.dto;

public record DocumentLinksResponse(
    String localPdf,
    String officialPdf,
    String officialXml
) {
}
