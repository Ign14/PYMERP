package com.datakomerz.pymes.inventory;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryLocationRepository extends JpaRepository<InventoryLocation, UUID> {

  @Query("""
      SELECT l FROM InventoryLocation l
      WHERE l.companyId = :companyId
      AND (:enabled IS NULL OR l.enabled = :enabled)
      AND (
        :query IS NULL OR
        LOWER(l.name) LIKE LOWER(CONCAT('%', :query, '%')) OR
        (l.code IS NOT NULL AND LOWER(l.code) LIKE LOWER(CONCAT('%', :query, '%')))
      )
      """)
  Page<InventoryLocation> search(
      @Param("companyId") UUID companyId,
      @Param("enabled") Boolean enabled,
      @Param("query") String query,
      Pageable pageable);

  boolean existsByCompanyIdAndName(UUID companyId, String name);

  boolean existsByCompanyIdAndCode(UUID companyId, String code);

  boolean existsByCompanyIdAndNameAndIdNot(UUID companyId, String name, UUID id);

  boolean existsByCompanyIdAndCodeAndIdNot(UUID companyId, String code, UUID id);

  Optional<InventoryLocation> findByCompanyIdAndCode(UUID companyId, String code);
}
