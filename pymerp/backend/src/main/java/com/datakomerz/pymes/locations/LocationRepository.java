package com.datakomerz.pymes.locations;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {
    List<Location> findByCompanyId(UUID companyId);
    List<Location> findByCompanyIdAndType(UUID companyId, LocationType type);
    List<Location> findByCompanyIdAndStatus(UUID companyId, LocationStatus status);
    List<Location> findByCompanyIdAndTypeAndStatus(UUID companyId, LocationType type, LocationStatus status);
    boolean existsByCompanyIdAndCode(UUID companyId, String code);
}
