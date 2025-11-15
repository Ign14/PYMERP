package com.datakomerz.pymes.requests;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRequestRepository extends JpaRepository<AccountRequest, UUID> {
  List<AccountRequest> findByStatusOrderByCreatedAtDesc(AccountRequestStatus status);

  @Query("select r from AccountRequest r where r.createdAt >= :since order by r.createdAt desc")
  List<AccountRequest> findRecentRequests(@Param("since") OffsetDateTime since);
}
