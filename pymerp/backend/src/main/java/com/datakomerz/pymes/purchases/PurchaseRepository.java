package com.datakomerz.pymes.purchases;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {
  @Query("""
      SELECT p
      FROM Purchase p
      LEFT JOIN Supplier s ON s.id = p.supplierId AND s.companyId = :companyId
      WHERE p.companyId = :companyId
        AND (:status IS NULL OR lower(p.status) = lower(:status))
        AND (:docType IS NULL OR lower(p.docType) = lower(:docType))
        AND (:from IS NULL OR p.issuedAt >= :from)
        AND (:to IS NULL OR p.issuedAt <= :to)
        AND (
          :search IS NULL
          OR lower(coalesce(p.docNumber, '')) LIKE lower(concat('%', :search, '%'))
          OR lower(coalesce(s.name, '')) LIKE lower(concat('%', :search, '%'))
        )
      ORDER BY p.issuedAt DESC
    """)
  Page<Purchase> search(@Param("companyId") UUID companyId,
                        @Param("status") String status,
                        @Param("docType") String docType,
                        @Param("search") String search,
                        @Param("from") OffsetDateTime from,
                        @Param("to") OffsetDateTime to,
                        Pageable pageable);

  Page<Purchase> findByCompanyIdOrderByIssuedAtDesc(UUID companyId, Pageable pageable);

  List<Purchase> findByCompanyIdAndIssuedAtGreaterThanEqualOrderByIssuedAtAsc(UUID companyId, OffsetDateTime issuedAt);

  Optional<Purchase> findByIdAndCompanyId(UUID id, UUID companyId);
}
