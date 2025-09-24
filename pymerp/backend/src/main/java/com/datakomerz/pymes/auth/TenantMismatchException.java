package com.datakomerz.pymes.auth;

import org.springframework.security.access.AccessDeniedException;

public class TenantMismatchException extends AccessDeniedException {

  private final String errorCode;

  public TenantMismatchException(String message) {
    this("TENANT_MISMATCH", message);
  }

  public TenantMismatchException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
