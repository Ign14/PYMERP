package com.datakomerz.pymes.billing.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "billing.webhook")
public class BillingWebhookProperties {

  /**
   * Shared secret used to validate incoming webhook signatures.
   */
  private String secret;

  /**
   * Maximum accepted age difference between the webhook timestamp and the current clock.
   * Defaults to five minutes.
   */
  private Duration tolerance = Duration.ofMinutes(5);

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public Duration getTolerance() {
    return tolerance;
  }

  public void setTolerance(Duration tolerance) {
    if (tolerance != null) {
      this.tolerance = tolerance;
    }
  }
}
