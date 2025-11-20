package com.datakomerz.pymes.locations.dto;

import com.datakomerz.pymes.locations.LocationStatus;
import com.datakomerz.pymes.locations.LocationType;

public record LocationReq(
    String code,
    String name,
    LocationType type,
    String businessName,
    String rut,
    String description,
    LocationStatus status
) {}
