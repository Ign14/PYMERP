package com.datakomerz.pymes.billing.provider;

public class BillingProviderTransientException extends BillingProviderException {

  public BillingProviderTransientException(String message) {
    super(message);
  }

  public BillingProviderTransientException(String message, Throwable cause) {
    super(message, cause);
  }
}
