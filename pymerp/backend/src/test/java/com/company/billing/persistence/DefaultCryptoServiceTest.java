package com.company.billing.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultCryptoServiceTest {

  private DefaultCryptoService cryptoService;

  @BeforeEach
  void setUp() {
    CryptoProperties properties = new CryptoProperties();
    properties.setSecret("MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=");
    cryptoService = new DefaultCryptoService(properties);
    cryptoService.init();
  }

  @Test
  void shouldEncryptAndDecryptRoundTrip() {
    byte[] payload = "factura-offline-demo".getBytes(StandardCharsets.UTF_8);

    byte[] encrypted = cryptoService.encrypt(payload);
    byte[] decrypted = cryptoService.decrypt(encrypted);

    assertThat(encrypted).isNotNull().isNotEmpty().isNotEqualTo(payload);
    assertThat(decrypted).isEqualTo(payload);
  }

  @Test
  void shouldHandleEmptyPayloadGracefully() {
    assertThat(cryptoService.encrypt(new byte[0])).isEmpty();
    assertThat(cryptoService.decrypt(new byte[0])).isEmpty();
    assertThat(cryptoService.encrypt(null)).isNull();
    assertThat(cryptoService.decrypt(null)).isNull();
  }

  @Test
  void shouldRejectMalformedPayload() {
    byte[] malformed = new byte[] {0x00, 0x01};
    assertThatThrownBy(() -> cryptoService.decrypt(malformed))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("malformed");
  }
}
