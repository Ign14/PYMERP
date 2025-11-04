package com.datakomerz.pymes.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {
  List<InventoryMovement> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
}
