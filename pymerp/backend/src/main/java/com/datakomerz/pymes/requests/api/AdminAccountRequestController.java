package com.datakomerz.pymes.requests.api;

import com.datakomerz.pymes.requests.AccountRequest;
import com.datakomerz.pymes.requests.AccountRequestStatus;
import com.datakomerz.pymes.requests.application.AccountRequestAdminService;
import com.datakomerz.pymes.requests.dto.AccountRequestAdminView;
import com.datakomerz.pymes.security.AppUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/account-requests")
public class AdminAccountRequestController {

  private final AccountRequestAdminService adminService;

  public AdminAccountRequestController(AccountRequestAdminService adminService) {
    this.adminService = adminService;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
  public List<AccountRequestAdminView> list(@RequestParam(required = false) AccountRequestStatus status,
                                            @RequestParam(defaultValue = "30") int days) {
    List<AccountRequest> requests = status == AccountRequestStatus.PENDING
      ? adminService.getPendingRequests()
      : adminService.getRecentRequests(days);
    return requests.stream().map(this::toView).toList();
  }

  @PostMapping("/{id}/approve")
  @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
  public ResponseEntity<Void> approve(@PathVariable UUID id,
                                      @AuthenticationPrincipal AppUserDetails currentUser) {
    adminService.approve(id, currentUser.getAccount());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{id}/reject")
  @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
  public ResponseEntity<Void> reject(@PathVariable UUID id,
                                     @Valid @RequestBody RejectRequest payload,
                                     @AuthenticationPrincipal AppUserDetails currentUser) {
    adminService.reject(id, currentUser.getAccount(), payload.reason());
    return ResponseEntity.ok().build();
  }

  private AccountRequestAdminView toView(AccountRequest request) {
    return new AccountRequestAdminView(
      request.getId(),
      request.getRut(),
      request.getFullName(),
      request.getEmail(),
      request.getCompanyName(),
      request.getAddress(),
      request.getStatus(),
      request.getCreatedAt(),
      request.getProcessedAt(),
      request.getProcessedBy(),
      request.getProcessedByUsername(),
      request.getRejectionReason(),
      request.getIpAddress(),
      request.getUserAgent()
    );
  }

  public record RejectRequest(
    @NotBlank(message = "Debes indicar el motivo del rechazo")
    String reason
  ) {}
}
