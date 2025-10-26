package com.company.billing.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NonFiscalDocumentRepository extends JpaRepository<NonFiscalDocument, UUID> {

  List<NonFiscalDocument> findBySale_Id(UUID saleId);

  Optional<NonFiscalDocument> findTopBySale_CompanyIdOrderByCreatedAtDesc(UUID companyId);
}
