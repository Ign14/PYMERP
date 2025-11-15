package com.datakomerz.pymes.inventory;

import com.datakomerz.pymes.inventory.dto.StockByLocationAggregation;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryLotRepository extends JpaRepository<InventoryLot, UUID> {
  List<InventoryLot> findByCompanyIdAndProductIdOrderByCreatedAtAsc(UUID companyId, UUID productId);
  List<InventoryLot> findByCompanyIdAndProductIdAndQtyAvailableGreaterThanOrderByExpDateAscCreatedAtAsc(
      UUID companyId, UUID productId, BigDecimal minQty);
  List<InventoryLot> findByCompanyIdAndProductIdAndLocationIdAndQtyAvailableGreaterThanOrderByExpDateAscCreatedAtAsc(
      UUID companyId, UUID productId, UUID locationId, BigDecimal minQty);
  List<InventoryLot> findTop5ByCompanyIdAndQtyAvailableLessThanOrderByQtyAvailableAsc(UUID companyId, BigDecimal threshold);
  List<InventoryLot> findByPurchaseItemId(UUID purchaseItemId);
  long countByCompanyIdAndQtyAvailableLessThan(UUID companyId, BigDecimal threshold);
  List<InventoryLot> findByCompanyIdAndLocationId(UUID companyId, UUID locationId);
  long countByCompanyIdAndLocationId(UUID companyId, UUID locationId);

  @Query("""
    SELECT COALESCE(SUM(l.qtyAvailable * COALESCE(l.costUnit, 0)), 0)
    FROM InventoryLot l
    WHERE l.companyId = :companyId
  """)
  BigDecimal sumInventoryValue(@Param("companyId") UUID companyId);

  @Query("""
    SELECT new com.datakomerz.pymes.inventory.dto.StockByLocationAggregation(
      l.productId, l.locationId, SUM(l.qtyAvailable)
    )
    FROM InventoryLot l
    WHERE l.companyId = :companyId
      AND l.locationId IS NOT NULL
      AND (:productIds IS NULL OR l.productId IN :productIds)
    GROUP BY l.productId, l.locationId
  """)
  List<StockByLocationAggregation> aggregateStockByProductAndLocation(
      @Param("companyId") UUID companyId,
      @Param("productIds") List<UUID> productIds);
}
