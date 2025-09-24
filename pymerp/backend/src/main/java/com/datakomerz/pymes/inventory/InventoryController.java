package com.datakomerz.pymes.inventory;

import com.datakomerz.pymes.inventory.dto.InventoryAdjustmentRequest;
import com.datakomerz.pymes.inventory.dto.InventoryAdjustmentResponse;
import com.datakomerz.pymes.inventory.dto.InventoryAlert;
import com.datakomerz.pymes.inventory.dto.InventorySettingsResponse;
import com.datakomerz.pymes.inventory.dto.InventorySettingsUpdateRequest;
import com.datakomerz.pymes.inventory.dto.InventorySummary;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

  private final InventoryService inventoryService;

  public InventoryController(InventoryService inventoryService) {
    this.inventoryService = inventoryService;
  }

  @GetMapping("/alerts")
  public List<InventoryAlert> lowStock(@RequestParam(required = false) BigDecimal threshold) {
    return inventoryService.lowStock(threshold);
  }

  @GetMapping("/summary")
  public InventorySummary summary() {
    return inventoryService.summary();
  }

  @GetMapping("/settings")
  public InventorySettingsResponse settings() {
    return inventoryService.getSettings();
  }

  @PutMapping("/settings")
  public InventorySettingsResponse updateSettings(@Valid @RequestBody InventorySettingsUpdateRequest request) {
    return inventoryService.updateSettings(request);
  }

  @PostMapping("/adjustments")
  public InventoryAdjustmentResponse adjust(@Valid @RequestBody InventoryAdjustmentRequest request) {
    return inventoryService.adjust(request);
  }
}
