package com.datakomerz.pymes.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {
  List<InventoryMovement> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
  
  @Query("""
    SELECT m FROM InventoryMovement m
    WHERE m.companyId = :companyId
    AND (:productId IS NULL OR m.productId = :productId)
    AND (:lotId IS NULL OR m.lotId = :lotId)
    AND (:type IS NULL OR m.type = :type)
    AND (:dateFrom IS NULL OR m.createdAt >= :dateFrom)
    AND (:dateTo IS NULL OR m.createdAt <= :dateTo)
    ORDER BY m.createdAt DESC
  """)
  Page<InventoryMovement> findMovementsWithFilters(
    @Param("companyId") UUID companyId,
    @Param("productId") UUID productId,
    @Param("lotId") UUID lotId,
    @Param("type") String type,
    @Param("dateFrom") OffsetDateTime dateFrom,
    @Param("dateTo") OffsetDateTime dateTo,
    Pageable pageable
  );
}
