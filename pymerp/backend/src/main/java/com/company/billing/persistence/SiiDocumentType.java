package com.company.billing.persistence;

public enum SiiDocumentType {
  FACTURA_ELECTRONICA(33),
  FACTURA_NO_AFECTA_O_EXENTA(34),
  BOLETA_ELECTRONICA(39),
  BOLETA_NO_AFECTA_O_EXENTA(41);

  private final int code;

  SiiDocumentType(int code) {
    this.code = code;
  }

  public int code() {
    return code;
  }

  public static SiiDocumentType from(FiscalDocumentType type, TaxMode taxMode) {
    if (type == null) {
      throw new IllegalArgumentException("Fiscal document type is required to resolve SII type");
    }
    TaxMode normalized = taxMode != null ? taxMode : TaxMode.AFECTA;
    return switch (type) {
      case FACTURA -> (normalized == TaxMode.EXENTA)
          ? FACTURA_NO_AFECTA_O_EXENTA
          : FACTURA_ELECTRONICA;
      case BOLETA -> (normalized == TaxMode.EXENTA)
          ? BOLETA_NO_AFECTA_O_EXENTA
          : BOLETA_ELECTRONICA;
    };
  }
}
