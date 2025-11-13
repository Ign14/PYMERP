package com.datakomerz.pymes.services.dto;

import com.datakomerz.pymes.services.ServiceStatus;
import java.math.BigDecimal;

public record ServiceReq(
    String code,
    String name,
    String description,
    String category,
    BigDecimal unitPrice,
    ServiceStatus status
) {}
