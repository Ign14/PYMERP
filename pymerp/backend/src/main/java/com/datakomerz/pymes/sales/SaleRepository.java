package com.datakomerz.pymes.sales;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleRepository extends JpaRepository<Sale, UUID> {
  @Query("""
      SELECT s
      FROM Sale s
      LEFT JOIN Customer c ON c.id = s.customerId AND c.companyId = :companyId
      WHERE s.companyId = :companyId
        AND (:status IS NULL OR lower(s.status) = lower(:status))
        AND (:docType IS NULL OR lower(s.docType) = lower(:docType))
        AND (:paymentMethod IS NULL OR lower(s.paymentMethod) = lower(:paymentMethod))
        AND (:from IS NULL OR s.issuedAt >= :from)
        AND (:to IS NULL OR s.issuedAt <= :to)
        AND (
          :search IS NULL
          OR lower(coalesce(s.docType, '')) LIKE lower(concat('%', :search, '%'))
          OR lower(coalesce(s.paymentMethod, '')) LIKE lower(concat('%', :search, '%'))
          OR lower(coalesce(c.name, '')) LIKE lower(concat('%', :search, '%'))
          OR cast(s.id as string) LIKE concat('%', :search, '%')
        )
      ORDER BY s.issuedAt DESC
    """)
  Page<Sale> search(@Param("companyId") UUID companyId,
                    @Param("status") String status,
                    @Param("docType") String docType,
                    @Param("paymentMethod") String paymentMethod,
                    @Param("search") String search,
                    @Param("from") OffsetDateTime from,
                    @Param("to") OffsetDateTime to,
                    Pageable pageable);

  List<Sale> findByCompanyIdAndIssuedAtGreaterThanEqualOrderByIssuedAtAsc(UUID companyId, OffsetDateTime issuedAt);

  List<Sale> findByCompanyIdAndIssuedAtBetweenOrderByIssuedAtAsc(UUID companyId, OffsetDateTime from, OffsetDateTime to);

  Page<Sale> findByCompanyIdOrderByIssuedAtDesc(UUID companyId, Pageable pageable);

  Optional<Sale> findByIdAndCompanyId(UUID id, UUID companyId);

  // Customer-specific queries
  Page<Sale> findByCompanyIdAndCustomerIdOrderByIssuedAtDesc(UUID companyId, UUID customerId, Pageable pageable);

  @Query("""
      SELECT COUNT(s)
      FROM Sale s
      WHERE s.companyId = :companyId AND s.customerId = :customerId
    """)
  Integer countByCompanyIdAndCustomerId(@Param("companyId") UUID companyId, @Param("customerId") UUID customerId);

  @Query("""
      SELECT COALESCE(SUM(s.total), 0)
      FROM Sale s
      WHERE s.companyId = :companyId AND s.customerId = :customerId
    """)
  java.math.BigDecimal sumTotalByCompanyIdAndCustomerId(@Param("companyId") UUID companyId, @Param("customerId") UUID customerId);
}
