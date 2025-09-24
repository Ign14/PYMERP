package com.datakomerz.pymes.suppliers;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SupplierContactRepository extends JpaRepository<SupplierContact, UUID> {
  List<SupplierContact> findBySupplierId(UUID supplierId);
}
