package com.datakomerz.pymes.sales;

import java.util.Locale;

public enum SaleDocumentType {
  FACTURA("Factura"),
  BOLETA("Boleta"),
  COMPROBANTE("Comprobante");

  private final String label;

  SaleDocumentType(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public static SaleDocumentType from(String value) {
    if (value == null) {
      return FACTURA;
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      return FACTURA;
    }
    String normalized = trimmed.toUpperCase(Locale.ROOT);
    for (SaleDocumentType type : values()) {
      if (type.name().equalsIgnoreCase(normalized) || type.label.equalsIgnoreCase(trimmed)) {
        return type;
      }
    }
    return switch (normalized) {
      case "INVOICE", "INVOICING" -> FACTURA;
      case "TICKET" -> BOLETA;
      case "RECEIPT" -> COMPROBANTE;
      default -> FACTURA;
    };
  }
}
