package com.datakomerz.pymes.inventory;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventorySettingsRepository extends JpaRepository<InventorySettings, UUID> {
}
