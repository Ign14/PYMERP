package com.datakomerz.pymes.billing.service;

import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpBillingWebhookFileClient implements BillingWebhookFileClient {

  private final RestClient restClient;

  public HttpBillingWebhookFileClient(RestClient.Builder builder) {
    this.restClient = builder.build();
  }

  @Override
  public byte[] download(String url) throws IOException {
    try {
      return restClient.get()
          .uri(url)
          .retrieve()
          .body(byte[].class);
    } catch (RestClientException ex) {
      throw new IOException("Unable to download billing file from " + url, ex);
    }
  }
}
