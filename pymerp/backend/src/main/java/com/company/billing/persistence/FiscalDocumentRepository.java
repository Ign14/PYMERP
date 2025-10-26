package com.company.billing.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FiscalDocumentRepository extends JpaRepository<FiscalDocument, UUID> {

  List<FiscalDocument> findBySale_Id(UUID saleId);

  List<FiscalDocument> findByStatus(FiscalDocumentStatus status);

  Optional<FiscalDocument> findByProvisionalNumber(String provisionalNumber);

  Optional<FiscalDocument> findByIdempotencyKey(String idempotencyKey);

  Optional<FiscalDocument>
      findTopBySale_CompanyIdAndProvisionalNumberStartingWithOrderByProvisionalNumberDesc(
          UUID companyId, String provisionalPrefix);
}
