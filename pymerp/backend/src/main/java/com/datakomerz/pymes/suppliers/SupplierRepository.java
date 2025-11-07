package com.datakomerz.pymes.suppliers;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
  List<Supplier> findAllByOrderByNameAsc();

  @Query(
    value = """
    SELECT s FROM Supplier s
    WHERE (:active IS NULL OR s.active = :active)
      AND (:search IS NULL OR :search = ''
           OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(s.rut) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(s.phone) LIKE LOWER(CONCAT('%', :search, '%')))
    ORDER BY s.name ASC
    """,
    countQuery = """
    SELECT COUNT(s) FROM Supplier s
    WHERE (:active IS NULL OR s.active = :active)
      AND (:search IS NULL OR :search = ''
           OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(s.rut) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(s.phone) LIKE LOWER(CONCAT('%', :search, '%')))
    """
  )
  Page<Supplier> searchSuppliers(
      @Param("active") Boolean active,
      @Param("search") String search,
      Pageable pageable);
}
