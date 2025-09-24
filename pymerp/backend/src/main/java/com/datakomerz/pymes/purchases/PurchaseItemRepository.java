package com.datakomerz.pymes.purchases;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, UUID> {
  List<PurchaseItem> findByPurchaseId(UUID purchaseId);
}
