package com.datakomerz.pymes.auth.api;

import com.datakomerz.pymes.auth.UserAccount;
import com.datakomerz.pymes.auth.application.UserAccountAdminService;
import com.datakomerz.pymes.auth.dto.CreateUserAccountRequest;
import com.datakomerz.pymes.auth.dto.UpdateUserAccountRequest;
import com.datakomerz.pymes.auth.dto.UpdateUserPasswordRequest;
import com.datakomerz.pymes.auth.dto.UserAccountResponse;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserAccountController {

  private final UserAccountAdminService service;

  public UserAccountController(UserAccountAdminService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_SETTINGS')")
  public List<UserAccountResponse> list() {
    return service.listForCurrentCompany().stream()
      .map(this::toResponse)
      .collect(Collectors.toList());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_SETTINGS')")
  public UserAccountResponse create(@Valid @RequestBody CreateUserAccountRequest request) {
    UserAccount account = service.create(request);
    return toResponse(account);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_SETTINGS')")
  public UserAccountResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserAccountRequest request) {
    UserAccount account = service.update(id, request);
    return toResponse(account);
  }

  @PostMapping("/{id}/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_SETTINGS')")
  public void updatePassword(@PathVariable UUID id, @Valid @RequestBody UpdateUserPasswordRequest request) {
    service.updatePassword(id, request);
  }

  private UserAccountResponse toResponse(UserAccount account) {
    List<String> roles = Arrays.stream(account.getRoles() != null ? account.getRoles().split(",") : new String[0])
      .map(String::trim)
      .filter(role -> !role.isEmpty())
      .collect(Collectors.toList());
    return new UserAccountResponse(
      account.getId(),
      account.getEmail(),
      account.getName(),
      account.getRole(),
      account.getStatus(),
      roles,
      account.getCreatedAt()
    );
  }
}
