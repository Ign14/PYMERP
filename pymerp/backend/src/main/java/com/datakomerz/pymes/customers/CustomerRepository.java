package com.datakomerz.pymes.customers;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
  Page<Customer> findByCompanyIdAndNameContainingIgnoreCase(UUID companyId, String name, Pageable pageable);

  Page<Customer> findByCompanyIdAndSegmentIgnoreCaseAndNameContainingIgnoreCase(
    UUID companyId,
    String segment,
    String name,
    Pageable pageable
  );

  Page<Customer> findByCompanyIdAndSegmentIsNullAndNameContainingIgnoreCase(
    UUID companyId,
    String name,
    Pageable pageable
  );

  Optional<Customer> findByIdAndCompanyId(UUID id, UUID companyId);


  @Query("""
    select c.segment as segment, count(c) as total
    from Customer c
    where c.companyId = :companyId
    group by c.segment
    order by count(c) desc
  """)
  List<CustomerSegmentView> summarizeBySegment(@Param("companyId") UUID companyId);

  interface CustomerSegmentView {
    String getSegment();
    long getTotal();
  }
}
