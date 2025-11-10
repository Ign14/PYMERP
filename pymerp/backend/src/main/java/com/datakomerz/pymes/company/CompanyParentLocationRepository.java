package com.datakomerz.pymes.company;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CompanyParentLocationRepository extends JpaRepository<CompanyParentLocation, UUID> {
  List<CompanyParentLocation> findByCompanyId(UUID companyId);
  void deleteByCompanyId(UUID companyId);
}
