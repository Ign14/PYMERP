package com.datakomerz.pymes.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceRepository extends JpaRepository<Service, UUID> {
    List<Service> findByCompanyId(UUID companyId);
    List<Service> findByCompanyIdAndActive(UUID companyId, Boolean active);
    Page<Service> findByCompanyId(UUID companyId, Pageable pageable);
    Page<Service> findByCompanyIdAndActive(UUID companyId, Boolean active, Pageable pageable);
    Optional<Service> findByCompanyIdAndCode(UUID companyId, String code);
    boolean existsByCompanyIdAndCode(UUID companyId, String code);
    long countByCompanyIdAndActive(UUID companyId, Boolean active);
}
