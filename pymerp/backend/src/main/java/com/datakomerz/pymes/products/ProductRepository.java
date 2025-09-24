package com.datakomerz.pymes.products;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {
  Page<Product> findByCompanyIdAndDeletedAtIsNullAndNameContainingIgnoreCase(UUID companyId, String q, Pageable pageable);
  Page<Product> findByCompanyIdAndDeletedAtIsNullAndSkuContainingIgnoreCase(UUID companyId, String q, Pageable pageable);
  Page<Product> findByCompanyIdAndDeletedAtIsNullAndBarcode(UUID companyId, String barcode, Pageable pageable);
  Page<Product> findByCompanyIdAndDeletedAtIsNullAndActiveIsAndNameContainingIgnoreCase(UUID companyId, Boolean active, String q, Pageable pageable);
  Page<Product> findByCompanyIdAndDeletedAtIsNullAndActiveIsAndSkuContainingIgnoreCase(UUID companyId, Boolean active, String q, Pageable pageable);
  Page<Product> findByCompanyIdAndDeletedAtIsNullAndActiveIsAndBarcode(UUID companyId, Boolean active, String barcode, Pageable pageable);
  Optional<Product> findByIdAndCompanyId(UUID id, UUID companyId);
  long countByCompanyIdAndDeletedAtIsNullAndActiveTrue(UUID companyId);
  long countByCompanyIdAndDeletedAtIsNullAndActiveFalse(UUID companyId);
}
