package com.datakomerz.pymes.customers;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

  @Query("""
    SELECT c FROM Customer c
    WHERE (:segment IS NULL OR c.segment = :segment)
      AND (:active IS NULL OR c.active = :active)
      AND (
        :search IS NULL OR :search = '' OR
        lower(c.name) LIKE lower(concat('%', :search, '%')) OR
        lower(c.email) LIKE lower(concat('%', :search, '%')) OR
        lower(c.phone) LIKE lower(concat('%', :search, '%')) OR
        lower(c.rut) LIKE lower(concat('%', :search, '%'))
      )
    ORDER BY c.createdAt DESC
  """)
  Page<Customer> searchCustomers(
    @Param("search") String search,
    @Param("segment") String segment,
    @Param("active") Boolean active,
    Pageable pageable
  );

  @Query(value = """
    SELECT * FROM customers c
    WHERE c.company_id = :companyId
      AND (:segment IS NULL OR c.segment = :segment)
      AND (
        :search IS NULL OR :search = '' OR
        lower(c.name) LIKE concat('%', lower(:search), '%') OR
        lower(c.email) LIKE concat('%', lower(:search), '%') OR
        lower(c.phone) LIKE concat('%', lower(:search), '%') OR
        lower(c.rut) LIKE concat('%', lower(:search), '%')
      )
    ORDER BY c.created_at DESC
  """,
  countQuery = """
    SELECT count(*) FROM customers c
    WHERE c.company_id = :companyId
      AND (:segment IS NULL OR c.segment = :segment)
      AND (
        :search IS NULL OR :search = '' OR
        lower(c.name) LIKE concat('%', lower(:search), '%') OR
        lower(c.email) LIKE concat('%', lower(:search), '%') OR
        lower(c.phone) LIKE concat('%', lower(:search), '%') OR
        lower(c.rut) LIKE concat('%', lower(:search), '%')
      )
  """,
  nativeQuery = true
  )
  Page<Customer> searchCustomersIncludingInactive(
    @Param("search") String search,
    @Param("segment") String segment,
    @Param("companyId") UUID companyId,
    Pageable pageable
  );

  @Query("""
    select c.segment as segment, count(c) as total
    from Customer c
    group by c.segment
    order by count(c) desc
  """)
  List<CustomerSegmentView> summarizeBySegment();

  interface CustomerSegmentView {
    String getSegment();
    long getTotal();
  }

  @Query("""
    SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
    FROM Customer c
    WHERE c.companyId = :companyId
      AND lower(c.email) = lower(:email)
      AND (:excludeId IS NULL OR c.id <> :excludeId)
  """)
  boolean existsByCompanyIdAndEmailIgnoreCase(
    @Param("companyId") UUID companyId,
    @Param("email") String email,
    @Param("excludeId") UUID excludeId
  );
}
