package com.datakomerz.pymes.locations.dto;

import com.datakomerz.pymes.locations.LocationType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record LocationWithHierarchy(
    UUID id,
    String code,
    String name,
    String description,
    LocationType type,
    UUID parentLocationId,
    Boolean active,
    Boolean isBlocked,
    BigDecimal capacity,
    String capacityUnit,
    BigDecimal currentCapacityUsed,
    String fullPath,  // Ej: "Bodega Principal > Pasillo A > Estante 1"
    int hierarchyLevel,  // Profundidad en la jerarquía (0 = raíz)
    int childrenCount,  // Cantidad de hijos directos
    int descendantsCount  // Cantidad total de descendientes
) {}
