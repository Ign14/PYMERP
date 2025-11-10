package com.datakomerz.pymes.company.dto;

import java.util.UUID;

public record ParentLocationResponse(
  UUID id,
  String name,
  String code
) {}
