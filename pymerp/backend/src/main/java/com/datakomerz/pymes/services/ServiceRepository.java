package com.datakomerz.pymes.services;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceRepository extends JpaRepository<Service, UUID> {
    List<Service> findByCompanyId(UUID companyId);
    List<Service> findByCompanyIdAndStatus(UUID companyId, ServiceStatus status);
    Page<Service> findByCompanyId(UUID companyId, Pageable pageable);
    Page<Service> findByCompanyIdAndStatus(UUID companyId, ServiceStatus status, Pageable pageable);
    Optional<Service> findByCompanyIdAndCode(UUID companyId, String code);
    boolean existsByCompanyIdAndCode(UUID companyId, String code);
    long countByCompanyIdAndStatus(UUID companyId, ServiceStatus status);
}
