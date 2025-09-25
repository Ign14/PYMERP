package com.datakomerz.pymes.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@TestConfiguration
public class TestJwtDecoderConfig {

  @Value("${app.security.jwt.secret}")
  private String jwtSecret;

  @Bean
  public JwtDecoder jwtDecoder() {
    byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
    SecretKeySpec key = new SecretKeySpec(keyBytes, "HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
  }
}
