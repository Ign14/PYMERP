package com.example.pymes.repository;

import com.example.pymes.entity.CustomerSegment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository for CustomerSegment entity.
 */
@Repository
public interface CustomerSegmentRepository extends JpaRepository<CustomerSegment, UUID> {

    /**
     * Find all active segments for a company.
     */
    List<CustomerSegment> findByCompanyIdAndActiveOrderByNameAsc(UUID companyId, Boolean active);

    /**
     * Find all segments for a company (active and inactive).
     */
    List<CustomerSegment> findByCompanyIdOrderByNameAsc(UUID companyId);

    /**
     * Find segment by company and code.
     */
    Optional<CustomerSegment> findByCompanyIdAndCode(UUID companyId, String code);

    /**
     * Check if a segment code exists for a company.
     */
    boolean existsByCompanyIdAndCode(UUID companyId, String code);

    /**
     * Count customers in each segment for a company.
     */
    @Query("""
        SELECT cs.code as code, cs.name as name, cs.color as color, COUNT(c.id) as total
        FROM CustomerSegment cs
        LEFT JOIN Customer c ON c.companyId = cs.companyId AND c.segment = cs.code
        WHERE cs.companyId = :companyId AND cs.active = true
        GROUP BY cs.code, cs.name, cs.color
        ORDER BY cs.name ASC
    """)
    List<Object[]> findSegmentStatsForCompany(UUID companyId);
}
