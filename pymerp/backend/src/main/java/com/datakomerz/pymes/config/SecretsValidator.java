package com.datakomerz.pymes.config;

import com.datakomerz.pymes.billing.config.BillingWebhookProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Validates that security-critical secrets are properly configured on application startup.
 * Prevents deployment with insecure default values.
 */
@Component
@ConditionalOnProperty(name = "app.security.secrets-validation.enabled", havingValue = "true", matchIfMissing = false)
public class SecretsValidator implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(SecretsValidator.class);

  private final SecurityProperties securityProperties;
  private final BillingWebhookProperties billingWebhookProperties;
  private final Environment environment;

  public SecretsValidator(
    SecurityProperties securityProperties,
    BillingWebhookProperties billingWebhookProperties,
    Environment environment
  ) {
    this.securityProperties = securityProperties;
    this.billingWebhookProperties = billingWebhookProperties;
    this.environment = environment;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    log.info("Validating security secrets configuration...");

    List<String> errors = new ArrayList<>();
    validateJwt(errors);
    validateBillingWebhook(errors);
    validateStorageSecrets(errors);
    validateRedisSecrets(errors);

    if (!errors.isEmpty()) {
      String errorMessage = String.join("\n  - ", errors);
      log.error("Security validation FAILED:\n  - {}", errorMessage);
      throw new IllegalStateException(
        "Application cannot start with insecure configuration:\n  - " + errorMessage +
        "\n\nPlease configure the required secrets in environment variables or application.yml"
      );
    }

    log.info("✅ Security secrets validation passed");
  }

  private void validateJwt(List<String> errors) {
    String secret = securityProperties.getJwt().getSecret();
    checkSecret("JWT_SECRET", secret, 32, errors);
  }

  private void validateBillingWebhook(List<String> errors) {
    String secret = billingWebhookProperties.getSecret();
    checkSecret("BILLING_WEBHOOK_SECRET", secret, 24, errors);
  }

  private void validateStorageSecrets(List<String> errors) {
    checkSecret("STORAGE_S3_BUCKET", environment.getProperty("STORAGE_S3_BUCKET"), 1, errors);
    checkSecret("STORAGE_S3_ACCESS_KEY", environment.getProperty("STORAGE_S3_ACCESS_KEY"), 1, errors);
    checkSecret("STORAGE_S3_SECRET_KEY", environment.getProperty("STORAGE_S3_SECRET_KEY"), 1, errors);
  }

  private void validateRedisSecrets(List<String> errors) {
    checkSecret("REDIS_HOST", environment.getProperty("REDIS_HOST"), 1, errors);
    checkPort("REDIS_PORT", environment.getProperty("REDIS_PORT"), errors);
    checkSecret("REDIS_PASSWORD", environment.getProperty("REDIS_PASSWORD"), 1, errors);
  }

  private void checkSecret(String name, String value, int minLength, List<String> errors) {
    if (value == null || value.isBlank()) {
      errors.add("Secret " + name + " is missing or weak (blank)");
      return;
    }

    if (minLength > 0 && value.length() < minLength) {
      errors.add("Secret " + name + " is missing or weak (minimum " + minLength + " characters required)");
      return;
    }

    if (isPlaceholder(value)) {
      errors.add("Secret " + name + " is missing or weak (placeholder value detected)");
    }
  }

  private void checkPort(String name, String value, List<String> errors) {
    if (value == null || value.isBlank()) {
      errors.add("Secret " + name + " is missing or weak (blank)");
      return;
    }

    try {
      int port = Integer.parseInt(value);
      if (port <= 0 || port > 65535) {
        errors.add("Secret " + name + " is missing or weak (port must be 1-65535)");
      }
    } catch (NumberFormatException ignored) {
      errors.add("Secret " + name + " is missing or weak (must be a valid port)");
    }
  }

  private boolean isPlaceholder(String value) {
    String normalized = value.trim().toLowerCase(Locale.ENGLISH);
    if (normalized.isEmpty()) {
      return true;
    }

    if (normalized.equals("default") || normalized.startsWith("default")) {
      return true;
    }
    if (normalized.equals("change-me") || normalized.startsWith("change-me")) {
      return true;
    }
    if (normalized.startsWith("changeme")) {
      return true;
    }
    if (normalized.contains("change_me") || normalized.contains("changeme")) {
      return true;
    }
    if (normalized.contains("change-this") || normalized.contains("change_this")) {
      return true;
    }
    if (normalized.startsWith("example") || normalized.contains("example")) {
      return true;
    }
    if (normalized.startsWith("dev-change") || normalized.startsWith("dev_")) {
      return true;
    }
    if (normalized.contains("placeholder")) {
      return true;
    }

    return false;
  }
}
