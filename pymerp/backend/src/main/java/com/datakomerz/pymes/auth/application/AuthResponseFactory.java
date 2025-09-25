package com.datakomerz.pymes.auth.application;

import com.datakomerz.pymes.auth.dto.AuthResponse;
import com.datakomerz.pymes.security.AppUserDetails;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class AuthResponseFactory {

  private static final List<String> ALL_MODULES = List.of(
    "dashboard",
    "sales",
    "purchases",
    "inventory",
    "customers",
    "suppliers",
    "finances",
    "reports",
    "settings"
  );

  public AuthResponse build(AppUserDetails principal,
                            String token,
                            long expiresIn,
                            String refreshToken,
                            long refreshExpiresIn) {
    Set<String> roles = principal.getAuthorities().stream()
      .map(GrantedAuthority::getAuthority)
      .filter(authority -> authority != null && !authority.isBlank())
      .collect(Collectors.toCollection(LinkedHashSet::new));

    Set<String> modules = resolveModules(roles);

    return new AuthResponse(
      token,
      expiresIn,
      refreshToken,
      refreshExpiresIn,
      principal.getCompanyId(),
      principal.getUsername(),
      principal.getDisplayName(),
      roles,
      modules
    );
  }

  private Set<String> resolveModules(Collection<String> roles) {
    Set<String> modules = new LinkedHashSet<>();
    if (roles == null || roles.isEmpty()) {
      modules.add("dashboard");
      return modules;
    }
    for (String role : roles) {
      if (role == null) {
        continue;
      }
      String normalized = role.trim().toUpperCase(Locale.ROOT);
      switch (normalized) {
        case "ROLE_ADMIN" -> modules.addAll(ALL_MODULES);
        case "ROLE_SALES", "ROLE_SELLER" -> modules.addAll(List.of("dashboard", "sales", "customers", "reports"));
        case "ROLE_PURCHASES", "ROLE_BUYER" -> modules.addAll(List.of("dashboard", "purchases", "suppliers", "inventory", "reports"));
        case "ROLE_INVENTORY" -> modules.addAll(List.of("dashboard", "inventory", "suppliers", "reports"));
        case "ROLE_FINANCE" -> modules.addAll(List.of("dashboard", "finances", "reports"));
        case "ROLE_REPORTS" -> modules.addAll(List.of("dashboard", "reports"));
        case "ROLE_SETTINGS" -> modules.add("settings");
        default -> {
        }
      }
    }
    if (modules.isEmpty()) {
      modules.add("dashboard");
    }
    modules.add("dashboard");
    return modules;
  }
}
