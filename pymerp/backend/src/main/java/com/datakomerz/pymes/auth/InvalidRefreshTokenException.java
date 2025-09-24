package com.datakomerz.pymes.auth;

public class InvalidRefreshTokenException extends RuntimeException {

  private final String errorCode;

  public InvalidRefreshTokenException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
