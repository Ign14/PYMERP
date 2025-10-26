package com.datakomerz.pymes.billing.service;

import java.io.IOException;

public interface BillingWebhookFileClient {

  byte[] download(String url) throws IOException;
}
