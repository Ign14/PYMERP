package com.company.billing.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContingencyQueueItemRepository extends JpaRepository<ContingencyQueueItem, UUID> {

  List<ContingencyQueueItem> findByStatusOrderByCreatedAtAsc(ContingencyQueueStatus status);

  Optional<ContingencyQueueItem> findByIdempotencyKey(String idempotencyKey);

  List<ContingencyQueueItem> findTop20ByStatusInOrderByCreatedAtAsc(Collection<ContingencyQueueStatus> statuses);

  long countByStatus(ContingencyQueueStatus status);
}
