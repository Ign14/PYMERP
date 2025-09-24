package com.datakomerz.pymes.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {}
