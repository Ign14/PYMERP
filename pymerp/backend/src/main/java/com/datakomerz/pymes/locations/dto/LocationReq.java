package com.datakomerz.pymes.locations.dto;

import com.datakomerz.pymes.locations.LocationType;

import java.util.UUID;

public record LocationReq(
    String code,
    String name,
    String description,
    LocationType type,
    UUID parentLocationId
) {}
