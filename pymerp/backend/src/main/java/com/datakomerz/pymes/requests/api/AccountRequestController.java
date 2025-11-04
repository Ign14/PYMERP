package com.datakomerz.pymes.requests.api;

import com.datakomerz.pymes.requests.application.AccountRequestService;
import com.datakomerz.pymes.requests.dto.AccountRequestPayload;
import com.datakomerz.pymes.requests.dto.AccountRequestResponse;
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
  public ResponseEntity<AccountRequestResponse> create(@Valid @RequestBody AccountRequestPayload payload) {
    AccountRequestResponse response = service.create(payload);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
