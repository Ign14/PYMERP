package com.datakomerz.pymes.suppliers;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
  List<Supplier> findByCompanyIdOrderByNameAsc(UUID companyId);
  Optional<Supplier> findByIdAndCompanyId(UUID id, UUID companyId);

  @Query(
      """
    SELECT s FROM Supplier s
    WHERE s.companyId = :companyId
      AND (:active IS NULL OR s.active = :active)
      AND (:search IS NULL OR :search = ''
           OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(s.rut) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(s.phone) LIKE LOWER(CONCAT('%', :search, '%')))
    ORDER BY s.name ASC
  """)
  List<Supplier> searchSuppliers(
      @Param("companyId") UUID companyId,
      @Param("active") Boolean active,
      @Param("search") String search);
}

