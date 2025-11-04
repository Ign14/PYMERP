package com.datakomerz.pymes.auth.application;

import com.datakomerz.pymes.auth.UserAccount;
import com.datakomerz.pymes.auth.UserAccountRepository;
import com.datakomerz.pymes.auth.dto.CreateUserAccountRequest;
import com.datakomerz.pymes.auth.dto.UpdateUserAccountRequest;
import com.datakomerz.pymes.auth.dto.UpdateUserPasswordRequest;
import com.datakomerz.pymes.core.tenancy.CompanyContext;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class UserAccountAdminService {

  private static final Set<String> ALLOWED_ROLES = Set.of(
    "ROLE_ADMIN",
    "ROLE_SALES",
    "ROLE_SELLER",
    "ROLE_PURCHASES",
    "ROLE_BUYER",
    "ROLE_INVENTORY",
    "ROLE_FINANCE",
    "ROLE_REPORTS",
    "ROLE_SETTINGS"
  );

  private final UserAccountRepository repository;
  private final CompanyContext companyContext;
  private final PasswordEncoder passwordEncoder;

  public UserAccountAdminService(UserAccountRepository repository,
                                 CompanyContext companyContext,
                                 PasswordEncoder passwordEncoder) {
    this.repository = repository;
    this.companyContext = companyContext;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional(Transactional.TxType.SUPPORTS)
  public List<UserAccount> listForCurrentCompany() {
    UUID companyId = companyContext.require();
    return repository.findByCompanyIdOrderByCreatedAtAsc(companyId);
  }

  public UserAccount create(CreateUserAccountRequest request) {
    UUID companyId = companyContext.require();
    String email = normalizeEmail(request.email());
    ensureEmailAvailable(email, null);

    List<String> roles = normalizeRoles(request.roles());
    String status = normalizeStatus(request.status());

    UserAccount account = new UserAccount();
    account.setCompanyId(companyId);
    account.setEmail(email);
    account.setName(normalizeName(request.name()));
    account.setRoles(String.join(",", roles));
    account.setRole(resolvePrimaryRole(request.role(), roles));
    account.setStatus(status);
    account.setPasswordHash(passwordEncoder.encode(request.password()));

    return repository.save(account);
  }

  public UserAccount update(UUID id, UpdateUserAccountRequest request) {
    UUID companyId = companyContext.require();
    UserAccount account = repository.findByIdAndCompanyId(id, companyId)
      .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado para la cuenta actual"));

    String email = normalizeEmail(request.email());
    ensureEmailAvailable(email, account.getId());

    List<String> roles = normalizeRoles(request.roles());
    String status = normalizeStatus(request.status());

    account.setEmail(email);
    account.setName(normalizeName(request.name()));
    account.setRoles(String.join(",", roles));
    account.setRole(resolvePrimaryRole(request.role(), roles));
    account.setStatus(status);

    return repository.save(account);
  }

  public void updatePassword(UUID id, UpdateUserPasswordRequest request) {
    UUID companyId = companyContext.require();
    UserAccount account = repository.findByIdAndCompanyId(id, companyId)
      .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado para la cuenta actual"));

    account.setPasswordHash(passwordEncoder.encode(request.password()));
    repository.save(account);
  }

  private String normalizeEmail(String email) {
    if (!StringUtils.hasText(email)) {
      throw new IllegalArgumentException("El correo electr\u00f3nico es obligatorio");
    }
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private String normalizeName(String name) {
    if (!StringUtils.hasText(name)) {
      throw new IllegalArgumentException("El nombre es obligatorio");
    }
    return name.trim();
  }

  private void ensureEmailAvailable(String email, UUID currentId) {
    repository.findByEmailIgnoreCase(email).ifPresent(existing -> {
      if (currentId == null || !Objects.equals(existing.getId(), currentId)) {
        throw new IllegalArgumentException("Ya existe un usuario registrado con este correo electr\u00f3nico");
      }
    });
  }

  private List<String> normalizeRoles(List<String> requestedRoles) {
    List<String> source = (requestedRoles == null || requestedRoles.isEmpty())
      ? List.of("ROLE_ADMIN")
      : requestedRoles;

    LinkedHashSet<String> normalized = new LinkedHashSet<>();
    for (String role : source) {
      if (!StringUtils.hasText(role)) {
        continue;
      }
      String candidate = role.trim().toUpperCase(Locale.ROOT);
      if (!candidate.startsWith("ROLE_")) {
        candidate = "ROLE_" + candidate;
      }
      if (!ALLOWED_ROLES.contains(candidate)) {
        throw new IllegalArgumentException("Rol no permitido: " + role);
      }
      normalized.add(candidate);
    }

    if (normalized.isEmpty()) {
      normalized.add("ROLE_ADMIN");
    }

    return normalized.stream().collect(Collectors.toList());
  }

  private String resolvePrimaryRole(String explicitRole, List<String> normalizedRoles) {
    if (StringUtils.hasText(explicitRole)) {
      return explicitRole.trim();
    }
    String firstRole = normalizedRoles.get(0);
    if (firstRole.startsWith("ROLE_") && firstRole.length() > 5) {
      return firstRole.substring(5).toLowerCase(Locale.ROOT);
    }
    return firstRole.toLowerCase(Locale.ROOT);
  }

  private String normalizeStatus(String status) {
    if (!StringUtils.hasText(status)) {
      return "active";
    }
    String normalized = status.trim().toLowerCase(Locale.ROOT);
    return switch (normalized) {
      case "active" -> "active";
      case "disabled", "inactive" -> "disabled";
      default -> throw new IllegalArgumentException("Estado no permitido: " + status);
    };
  }
}
