package com.datakomerz.pymes.sales;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleLotAllocationRepository extends JpaRepository<SaleLotAllocation, UUID> {
  List<SaleLotAllocation> findBySaleId(UUID saleId);
  void deleteBySaleId(UUID saleId);
}
