package com.datakomerz.pymes.common;

import org.springframework.lang.Nullable;

/**
 * Exception used to signal a single field validation error that should be surfaced in API responses.
 */
public class FieldValidationException extends RuntimeException {

  private final String field;
  private final String code;

  public FieldValidationException(String field, String message) {
    this(field, message, null);
  }

  public FieldValidationException(String field, String message, @Nullable String code) {
    super(message);
    this.field = field;
    this.code = code;
  }

  public String getField() {
    return field;
  }

  @Nullable
  public String getCode() {
    return code;
  }
}
