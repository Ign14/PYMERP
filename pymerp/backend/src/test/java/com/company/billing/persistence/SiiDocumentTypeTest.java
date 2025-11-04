package com.company.billing.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SiiDocumentTypeTest {

  @Test
  void facturaAfecta_mapsToFacturaElectronica() {
    SiiDocumentType result = SiiDocumentType.from(FiscalDocumentType.FACTURA, TaxMode.AFECTA);
    assertThat(result).isEqualTo(SiiDocumentType.FACTURA_ELECTRONICA);
  }

  @Test
  void facturaExenta_mapsToNoAfectaOExenta() {
    SiiDocumentType result = SiiDocumentType.from(FiscalDocumentType.FACTURA, TaxMode.EXENTA);
    assertThat(result).isEqualTo(SiiDocumentType.FACTURA_NO_AFECTA_O_EXENTA);
  }

  @Test
  void boletaAfecta_mapsToBoletaElectronica() {
    SiiDocumentType result = SiiDocumentType.from(FiscalDocumentType.BOLETA, TaxMode.AFECTA);
    assertThat(result).isEqualTo(SiiDocumentType.BOLETA_ELECTRONICA);
  }

  @Test
  void boletaExenta_mapsToBoletaNoAfectaOExenta() {
    SiiDocumentType result = SiiDocumentType.from(FiscalDocumentType.BOLETA, TaxMode.EXENTA);
    assertThat(result).isEqualTo(SiiDocumentType.BOLETA_NO_AFECTA_O_EXENTA);
  }

  @Test
  void nullTaxMode_defaultsToAfecta() {
    SiiDocumentType result = SiiDocumentType.from(FiscalDocumentType.FACTURA, null);
    assertThat(result).isEqualTo(SiiDocumentType.FACTURA_ELECTRONICA);
  }

  @Test
  void nullType_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> SiiDocumentType.from(null, TaxMode.AFECTA));
  }
}
