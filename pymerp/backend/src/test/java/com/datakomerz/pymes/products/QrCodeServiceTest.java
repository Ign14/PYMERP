package com.datakomerz.pymes.products;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class QrCodeServiceTest {
  private final QrCodeService service = new QrCodeService();

  @Test
  void generatesPngImage() {
    QrCodeService.GeneratedQr qr = service.generate("SKU-12345");
    assertThat(qr.content()).isNotNull();
    assertThat(qr.content().length).isGreaterThan(0);
    assertThat(qr.extension()).isEqualTo("png");
    assertThat(qr.contentType()).isEqualTo("image/png");
  }
}
