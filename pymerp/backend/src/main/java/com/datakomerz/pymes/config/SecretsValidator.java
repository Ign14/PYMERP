package com.datakomerz.pymes.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates that security-critical secrets are properly configured on application startup.
 * Prevents deployment with insecure default values.
 */
@Component
public class SecretsValidator implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(SecretsValidator.class);
  
  private final AppProperties appProperties;

  public SecretsValidator(AppProperties appProperties) {
    this.appProperties = appProperties;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    log.info("Validating security secrets configuration...");
    
    List<String> errors = new ArrayList<>();
    
    // Validate JWT secret
    String jwtSecret = appProperties.getSecurity().getJwt().getSecret();
    if (jwtSecret == null || jwtSecret.isBlank()) {
      errors.add("JWT_SECRET is not configured");
    } else if (jwtSecret.length() < 32) {
      errors.add("JWT_SECRET must be at least 32 characters (current: " + jwtSecret.length() + ")");
    } else if (jwtSecret.contains("change") || jwtSecret.contains("dev-secret") || jwtSecret.contains("example")) {
      errors.add("JWT_SECRET contains insecure default value. Must be randomly generated.");
    }
    
    // Validate billing webhook secret (if billing is enabled)
    // Note: BillingWebhookProperties might not be autowired here, so we skip for now
    // This validation could be done in BillingWebhookSignatureVerifier
    
    if (!errors.isEmpty()) {
      String errorMessage = String.join("\n  - ", errors);
      log.error("Security validation FAILED:\n  - {}", errorMessage);
      throw new IllegalStateException(
        "Application cannot start with insecure configuration:\n  - " + errorMessage + 
        "\n\nPlease configure proper secrets in environment variables or application.yml"
      );
    }
    
    log.info("✅ Security secrets validation passed");
  }
}
