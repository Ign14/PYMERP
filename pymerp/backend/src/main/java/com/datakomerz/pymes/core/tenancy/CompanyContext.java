package com.datakomerz.pymes.core.tenancy;

import org.springframework.stereotype.Component;

import com.datakomerz.pymes.multitenancy.TenantContext;
import com.datakomerz.pymes.multitenancy.TenantNotFoundException;

import java.util.Optional;
import java.util.UUID;

@Component
public class CompanyContext {

  void set(UUID companyId) {
    TenantContext.setTenantId(companyId);
  }

  public Optional<UUID> current() {
    return Optional.ofNullable(TenantContext.getTenantId());
  }

  public UUID require() {
    try {
      return TenantContext.require();
    } catch (TenantNotFoundException ex) {
      throw new IllegalStateException("Company could not be resolved for request", ex);
    }
  }

  void clear() {
    TenantContext.clear();
  }
}
