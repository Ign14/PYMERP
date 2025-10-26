package com.datakomerz.pymes.billing.render;

public class LocalInvoiceRenderingException extends RuntimeException {

  public LocalInvoiceRenderingException(String message) {
    super(message);
  }

  public LocalInvoiceRenderingException(String message, Throwable cause) {
    super(message, cause);
  }
}
