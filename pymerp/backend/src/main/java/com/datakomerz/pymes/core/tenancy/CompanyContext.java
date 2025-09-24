package com.datakomerz.pymes.core.tenancy;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class CompanyContext {

  private final ThreadLocal<UUID> current = new ThreadLocal<>();

  void set(UUID companyId) {
    current.set(companyId);
  }

  public Optional<UUID> current() {
    return Optional.ofNullable(current.get());
  }

  public UUID require() {
    return current().orElseThrow(() -> new IllegalStateException("Company could not be resolved for request"));
  }

  void clear() {
    current.remove();
  }
}
