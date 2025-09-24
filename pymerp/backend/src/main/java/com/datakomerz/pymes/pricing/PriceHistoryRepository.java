package com.datakomerz.pymes.pricing;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, UUID> {
  Page<PriceHistory> findByProductIdOrderByValidFromDesc(UUID productId, Pageable pageable);
  Optional<PriceHistory> findFirstByProductIdOrderByValidFromDesc(UUID productId);
}
