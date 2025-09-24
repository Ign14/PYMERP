package com.datakomerz.pymes.inventory;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryLotRepository extends JpaRepository<InventoryLot, UUID> {
  List<InventoryLot> findByCompanyIdAndProductIdOrderByCreatedAtAsc(UUID companyId, UUID productId);
  List<InventoryLot> findTop5ByCompanyIdAndQtyAvailableLessThanOrderByQtyAvailableAsc(UUID companyId, BigDecimal threshold);
  List<InventoryLot> findByPurchaseItemId(UUID purchaseItemId);
  long countByCompanyIdAndQtyAvailableLessThan(UUID companyId, BigDecimal threshold);

  @Query("SELECT COALESCE(SUM(l.qtyAvailable * COALESCE(l.costUnit, 0)), 0) FROM InventoryLot l WHERE l.companyId = :companyId")
  BigDecimal sumInventoryValue(@Param("companyId") UUID companyId);
}
