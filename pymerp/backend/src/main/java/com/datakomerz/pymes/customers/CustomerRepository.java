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
}
