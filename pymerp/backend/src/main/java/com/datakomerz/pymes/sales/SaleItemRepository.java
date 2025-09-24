package com.datakomerz.pymes.sales;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleItemRepository extends JpaRepository<SaleItem, UUID> {
  List<SaleItem> findBySaleId(UUID saleId);
}
