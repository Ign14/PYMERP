package com.datakomerz.pymes.billing.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BillingIdempotencyConfiguration {

  @Bean
  public BillingIdempotencyStore billingIdempotencyStore() {
    return new InMemoryBillingIdempotencyStore();
  }
}
