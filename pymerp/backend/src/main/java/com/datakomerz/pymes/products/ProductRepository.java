package com.datakomerz.pymes.products;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {
  Page<Product> findByDeletedAtIsNullAndNameContainingIgnoreCase(String q, Pageable pageable);
  Page<Product> findByDeletedAtIsNullAndSkuContainingIgnoreCase(String q, Pageable pageable);
  Page<Product> findByDeletedAtIsNullAndBarcode(String barcode, Pageable pageable);
  Page<Product> findByDeletedAtIsNullAndActiveIsAndNameContainingIgnoreCase(Boolean active, String q, Pageable pageable);
  Page<Product> findByDeletedAtIsNullAndActiveIsAndSkuContainingIgnoreCase(Boolean active, String q, Pageable pageable);
  Page<Product> findByDeletedAtIsNullAndActiveIsAndBarcode(Boolean active, String barcode, Pageable pageable);
  long countByDeletedAtIsNullAndActiveTrue();
  long countByDeletedAtIsNullAndActiveFalse();
  List<Product> findByIdIn(Collection<UUID> ids);
}
