package com.datakomerz.pymes.locations;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {
    List<Location> findByCompanyId(UUID companyId);
    List<Location> findByCompanyIdAndType(UUID companyId, LocationType type);
    List<Location> findByCompanyIdAndParentLocationId(UUID companyId, UUID parentLocationId);
    boolean existsByCompanyIdAndCode(UUID companyId, String code);
}
