package com.datakomerz.pymes.billing.provider;

public class BillingProviderException extends RuntimeException {

  public BillingProviderException(String message) {
    super(message);
  }

  public BillingProviderException(String message, Throwable cause) {
    super(message, cause);
  }
}
