package com.datakomerz.pymes.requests.api;

import com.datakomerz.pymes.requests.application.AccountRequestService;
import com.datakomerz.pymes.requests.dto.AccountRequestPayload;
import com.datakomerz.pymes.requests.dto.AccountRequestResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/requests")
public class AccountRequestController {

  private final AccountRequestService service;

  public AccountRequestController(AccountRequestService service) {
    this.service = service;
  }

  @PostMapping
  @PreAuthorize("permitAll()")
  public ResponseEntity<AccountRequestResponse> create(@Valid @RequestBody AccountRequestPayload payload,
                                                       HttpServletRequest servletRequest) {
    AccountRequestResponse response = service.create(payload, resolveIp(servletRequest), servletRequest.getHeader("User-Agent"));
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  private String resolveIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    String realIp = request.getHeader("X-Real-IP");
    if (realIp != null && !realIp.isBlank()) {
      return realIp.trim();
    }
    String remote = request.getRemoteAddr();
    return remote != null ? remote.trim() : null;
  }
}
