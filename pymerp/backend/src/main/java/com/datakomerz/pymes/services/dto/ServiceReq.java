package com.datakomerz.pymes.services.dto;

public record ServiceReq(
    String code,
    String name,
    String description,
    Boolean active
) {}
