package com.datakomerz.pymes.suppliers;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
  List<Supplier> findByCompanyIdOrderByNameAsc(UUID companyId);
  Optional<Supplier> findByIdAndCompanyId(UUID id, UUID companyId);
}

