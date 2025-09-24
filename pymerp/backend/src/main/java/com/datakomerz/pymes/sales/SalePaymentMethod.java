package com.datakomerz.pymes.sales;

import java.util.Locale;

public enum SalePaymentMethod {
  EFECTIVO("Efectivo"),
  TARJETAS("Tarjetas"),
  TRANSFERENCIA("Transferencia"),
  OTROS("Otros");

  private final String label;

  SalePaymentMethod(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public static SalePaymentMethod from(String value) {
    if (value == null) {
      return TRANSFERENCIA;
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      return TRANSFERENCIA;
    }
    String normalized = trimmed.toUpperCase(Locale.ROOT);
    for (SalePaymentMethod method : values()) {
      if (method.name().equalsIgnoreCase(normalized) || method.label.equalsIgnoreCase(trimmed)) {
        return method;
      }
    }
    return switch (normalized) {
      case "CASH" -> EFECTIVO;
      case "CARD", "CARDS" -> TARJETAS;
      case "WIRE", "BANK", "TRANSFER" -> TRANSFERENCIA;
      default -> OTROS;
    };
  }
}
