package com.datakomerz.pymes.billing.event;

import java.util.List;
import java.util.UUID;

public record DocumentRejected(
    UUID documentId,
    String provider,
    String externalId,
    String trackId,
    List<String> errors) {
}
