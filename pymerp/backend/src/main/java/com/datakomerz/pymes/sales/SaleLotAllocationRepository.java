package com.datakomerz.pymes.sales;

import com.datakomerz.pymes.inventory.dto.LotReservationSummary;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleLotAllocationRepository extends JpaRepository<SaleLotAllocation, UUID> {
  List<SaleLotAllocation> findBySaleId(UUID saleId);
  void deleteBySaleId(UUID saleId);

  @Query("""
    SELECT new com.datakomerz.pymes.inventory.dto.LotReservationSummary(a.lotId, COALESCE(SUM(a.qty), 0))
    FROM SaleLotAllocation a
    JOIN InventoryLot lot ON lot.id = a.lotId
    WHERE lot.companyId = :companyId
      AND a.lotId IN :lotIds
    GROUP BY a.lotId
  """)
  List<LotReservationSummary> sumReservedByLotIds(
    @Param("companyId") UUID companyId,
    @Param("lotIds") List<UUID> lotIds
  );
}
