package com.datakomerz.pymes.requests.dto;

import com.datakomerz.pymes.requests.AccountRequestStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountRequestAdminView(
  UUID id,
  String rut,
  String fullName,
  String email,
  String companyName,
  String address,
  AccountRequestStatus status,
  OffsetDateTime createdAt,
  OffsetDateTime processedAt,
  UUID processedBy,
  String processedByUsername,
  String rejectionReason,
  String ipAddress,
  String userAgent
) {}
