package com.datakomerz.pymes.common.captcha;

public class CaptchaValidationException extends RuntimeException {

  private final String type;
  private final String field;

  public CaptchaValidationException(String message, String type, String field) {
    super(message);
    this.type = type;
    this.field = field;
  }

  public String getType() {
    return type;
  }

  public String getField() {
    return field;
  }
}
