package com.datakomerz.pymes.locations.dto;

import com.datakomerz.pymes.locations.LocationType;

import java.math.BigDecimal;
import java.util.UUID;

public record LocationReq(
    String code,
    String name,
    String description,
    LocationType type,
    UUID parentLocationId,
    Boolean active,
    Boolean isBlocked,
    BigDecimal capacity,
    String capacityUnit
) {}
