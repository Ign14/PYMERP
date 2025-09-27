package com.datakomerz.pymes.common.validation;

public final class RutUtils {

  private RutUtils() {
  }

  public static boolean isValid(String rut) {
    if (rut == null) {
      return false;
    }
    String normalized = rut.replace(".", "").replace("-", "").trim().toUpperCase();
    if (!normalized.matches("\\d{7,8}[0-9K]")) {
      return false;
    }
    String body = normalized.substring(0, normalized.length() - 1);
    char dv = normalized.charAt(normalized.length() - 1);
    char expected = calculateCheckDigit(body);
    return dv == expected;
  }

  public static String normalize(String rut) {
    if (rut == null) {
      return null;
    }
    String normalized = rut.replace(".", "").replace("-", "").trim().toUpperCase();
    if (normalized.length() < 2) {
      return normalized;
    }
    String body = normalized.substring(0, normalized.length() - 1);
    char dv = normalized.charAt(normalized.length() - 1);
    return body + "-" + dv;
  }

  private static char calculateCheckDigit(String body) {
    int sum = 0;
    int factor = 2;
    for (int i = body.length() - 1; i >= 0; i--) {
      int digit = Character.digit(body.charAt(i), 10);
      sum += digit * factor;
      factor = factor == 7 ? 2 : factor + 1;
    }
    int modulus = 11 - (sum % 11);
    if (modulus == 11) {
      return '0';
    }
    if (modulus == 10) {
      return 'K';
    }
    return Character.forDigit(modulus, 10);
  }
}
