package com.datakomerz.pymes.auth;

import org.springframework.security.access.AccessDeniedException;

public class UserDisabledException extends AccessDeniedException {

  private final String errorCode;

  public UserDisabledException(String message) {
    this("USER_DISABLED", message);
  }

  public UserDisabledException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
