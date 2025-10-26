package com.datakomerz.pymes.billing.event;

import java.util.UUID;

public record DocumentAccepted(
    UUID documentId,
    String provider,
    String externalId,
    String number,
    String trackId) {
}
