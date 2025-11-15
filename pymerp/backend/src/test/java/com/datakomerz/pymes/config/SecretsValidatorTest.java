package com.datakomerz.pymes.config;

import com.datakomerz.pymes.billing.config.BillingWebhookProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecretsValidatorTest {

  private static final String VALID_JWT_SECRET = "example-jwt-secret-32-characters-long";
  private static final String VALID_BILLING_SECRET = "billing-secret-24chars-long!!";

  private static MockEnvironment validEnvironment() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("STORAGE_S3_BUCKET", "local-storage-bucket");
    environment.setProperty("STORAGE_S3_ACCESS_KEY", "localStorageAccessKey");
    environment.setProperty("STORAGE_S3_SECRET_KEY", "localStorageSecretKey");
    environment.setProperty("REDIS_HOST", "localhost");
    environment.setProperty("REDIS_PORT", "6379");
    environment.setProperty("REDIS_PASSWORD", "localRedisPassword");
    return environment;
  }

  private static SecurityProperties securityProperties(String secret) {
    SecurityProperties properties = new SecurityProperties();
    properties.getJwt().setSecret(secret);
    return properties;
  }

  private static BillingWebhookProperties billingWebhookProperties(String secret) {
    BillingWebhookProperties properties = new BillingWebhookProperties();
    properties.setSecret(secret);
    return properties;
  }

  @ParameterizedTest
  @ValueSource(strings = {"short", "length-31-characters-!!!"})
  void jwtSecretTooShortCausesFailure(String shortSecret) {
    SecretsValidator validator = new SecretsValidator(
      securityProperties(shortSecret),
      billingWebhookProperties(VALID_BILLING_SECRET),
      validEnvironment()
    );

    assertThrows(IllegalStateException.class, () -> validator.run(new DefaultApplicationArguments(new String[0])));
  }

  @ParameterizedTest
  @ValueSource(strings = {"change-me"})
  void billingWebhookSecretChangeMeFails(String billingSecret) {
    SecretsValidator validator = new SecretsValidator(
      securityProperties(VALID_JWT_SECRET),
      billingWebhookProperties(billingSecret),
      validEnvironment()
    );

    assertThrows(IllegalStateException.class, () -> validator.run(new DefaultApplicationArguments(new String[0])));
  }

  @Test
  void validSecretsAreAllowed() {
    SecretsValidator validator = new SecretsValidator(
      securityProperties(VALID_JWT_SECRET),
      billingWebhookProperties(VALID_BILLING_SECRET),
      validEnvironment()
    );

    assertDoesNotThrow(() -> validator.run(new DefaultApplicationArguments(new String[0])));
  }
}
