package com.datakomerz.pymes.common.payments;

import java.util.Arrays;

/**
 * Supported payment terms (in days) for sales and purchases.
 */
public enum PaymentTerm {
  DAYS_7(7),
  DAYS_15(15),
  DAYS_30(30),
  DAYS_60(60);

  private final int days;

  PaymentTerm(int days) {
    this.days = days;
  }

  public int getDays() {
    return days;
  }

  /**
   * Normalizes and validates the provided number of days, throwing an
   * {@link IllegalArgumentException} if the value is not supported.
   */
  public static int normalize(Integer days) {
    if (days == null) {
      throw new IllegalArgumentException("paymentTermDays is required");
    }
    return Arrays.stream(values())
        .filter(term -> term.days == days)
        .findFirst()
        .map(PaymentTerm::getDays)
        .orElseThrow(() -> new IllegalArgumentException(
            "paymentTermDays must be one of 7, 15, 30 or 60"));
  }

  public static PaymentTerm fromDays(int days) {
    return Arrays.stream(values())
        .filter(term -> term.days == days)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "Unsupported payment term: " + days));
  }
}
