package com.datakomerz.pymes.requests.dto;

import com.datakomerz.pymes.requests.AccountRequestStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountRequestResponse(
  UUID id,
  AccountRequestStatus status,
  OffsetDateTime createdAt,
  String message
) {
}
