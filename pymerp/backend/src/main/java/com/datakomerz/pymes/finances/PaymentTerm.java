package com.datakomerz.pymes.finances;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Términos de pago/cobro permitidos en el sistema.
 * Valores alineados con plazos comerciales estándar en Chile.
 */
public enum PaymentTerm {
  DAYS_7(7, "7 días"),
  DAYS_15(15, "15 días"),
  DAYS_30(30, "30 días"),
  DAYS_60(60, "60 días");

  private final int days;
  private final String label;

  PaymentTerm(int days, String label) {
    this.days = days;
    this.label = label;
  }

  @JsonValue
  public int getDays() {
    return days;
  }

  public String getLabel() {
    return label;
  }

  /**
   * Convierte un número de días a PaymentTerm.
   * @param days número de días (7, 15, 30, 60)
   * @return PaymentTerm correspondiente
   * @throws IllegalArgumentException si el valor no es válido
   */
  public static PaymentTerm fromDays(int days) {
    for (PaymentTerm term : values()) {
      if (term.days == days) {
        return term;
      }
    }
    throw new IllegalArgumentException(
      "Término de pago inválido: " + days + ". Valores permitidos: 7, 15, 30, 60 días"
    );
  }

  /**
   * Verifica si un número de días es un término válido.
   * @param days número de días a validar
   * @return true si es válido (7, 15, 30, 60)
   */
  public static boolean isValid(int days) {
    return days == 7 || days == 15 || days == 30 || days == 60;
  }
}
