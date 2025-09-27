package com.datakomerz.pymes.inventory;

import com.datakomerz.pymes.core.tenancy.CompanyContext;
import com.datakomerz.pymes.inventory.dto.InventoryAdjustmentRequest;
import com.datakomerz.pymes.inventory.dto.InventoryAdjustmentResponse;
import com.datakomerz.pymes.inventory.dto.InventoryAlert;
import com.datakomerz.pymes.inventory.dto.InventorySettingsResponse;
import com.datakomerz.pymes.inventory.dto.InventorySettingsUpdateRequest;
import com.datakomerz.pymes.inventory.dto.InventorySummary;
import com.datakomerz.pymes.products.ProductRepository;
import com.datakomerz.pymes.sales.SaleLotAllocation;
import com.datakomerz.pymes.sales.SaleLotAllocationRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
  private static final BigDecimal FALLBACK_THRESHOLD = new BigDecimal("5");

  private final InventoryLotRepository lots;
  private final InventoryMovementRepository movements;
  private final SaleLotAllocationRepository allocations;
  private final InventorySettingsRepository settingsRepository;
  private final ProductRepository productRepository;
  private final CompanyContext companyContext;

  public InventoryService(InventoryLotRepository lots,
                          InventoryMovementRepository movements,
                          SaleLotAllocationRepository allocations,
                          InventorySettingsRepository settingsRepository,
                          ProductRepository productRepository,
                          CompanyContext companyContext) {
    this.lots = lots;
    this.movements = movements;
    this.allocations = allocations;
    this.settingsRepository = settingsRepository;
    this.productRepository = productRepository;
    this.companyContext = companyContext;
  }

  @Transactional
  public void consumeFIFO(UUID saleId, UUID productId, BigDecimal qty) {
    UUID companyId = companyContext.require();
    var remaining = qty;
    var candidates = lots.findByCompanyIdAndProductIdOrderByCreatedAtAsc(companyId, productId);

    for (var lot : candidates) {
      if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
        break;
      }
      if (lot.getQtyAvailable().compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }

      var take = lot.getQtyAvailable().min(remaining);
      lot.setQtyAvailable(lot.getQtyAvailable().subtract(take));
      lots.save(lot);

      var movement = new InventoryMovement();
      movement.setCompanyId(companyId);
      movement.setProductId(productId);
      movement.setLotId(lot.getId());
      movement.setType("SALE_OUT");
      movement.setQty(take);
      movement.setRefType("SALE");
      movement.setRefId(saleId);
      movements.save(movement);

      var allocation = new SaleLotAllocation();
      allocation.setSaleId(saleId);
      allocation.setProductId(productId);
      allocation.setLotId(lot.getId());
      allocation.setQty(take);
      allocations.save(allocation);

      remaining = remaining.subtract(take);
    }

    if (remaining.compareTo(BigDecimal.ZERO) > 0) {
      throw new IllegalStateException("Insufficient stock for product " + productId + " (missing " + remaining + ")");
    }
  }

  @Transactional
  public void restockSale(UUID saleId) {
    UUID companyId = companyContext.require();
    List<SaleLotAllocation> saleAllocations = allocations.findBySaleId(saleId);
    if (saleAllocations.isEmpty()) {
      return;
    }
    for (SaleLotAllocation allocation : saleAllocations) {
      InventoryLot lot = lots.findById(allocation.getLotId())
        .orElseThrow(() -> new IllegalStateException("Lot not found for allocation " + allocation.getLotId()));
      lot.setQtyAvailable(lot.getQtyAvailable().add(allocation.getQty()));
      lots.save(lot);

      InventoryMovement movement = new InventoryMovement();
      movement.setCompanyId(companyId);
      movement.setProductId(allocation.getProductId());
      movement.setLotId(lot.getId());
      movement.setType("SALE_CANCEL");
      movement.setQty(allocation.getQty());
      movement.setRefType("SALE");
      movement.setRefId(saleId);
      movements.save(movement);
    }
    allocations.deleteBySaleId(saleId);
  }

  @Transactional(readOnly = true)
  public List<InventoryAlert> lowStock(BigDecimal thresholdOverride) {
    UUID companyId = companyContext.require();
    BigDecimal threshold = resolveThreshold(companyId, thresholdOverride);
    return lots.findTop5ByCompanyIdAndQtyAvailableLessThanOrderByQtyAvailableAsc(companyId, threshold)
      .stream()
      .map(lot -> new InventoryAlert(
        lot.getId(),
        lot.getProductId(),
        lot.getQtyAvailable(),
        lot.getCreatedAt(),
        lot.getExpDate()
      ))
      .toList();
  }

  @Transactional(readOnly = true)
  public List<InventoryLot> lotsForProduct(UUID productId) {
    UUID companyId = companyContext.require();
    validateProductBelongsToCompany(productId, companyId);
    return lots.findByCompanyIdAndProductIdOrderByCreatedAtAsc(companyId, productId);
  }

  @Transactional(readOnly = true)
  public InventorySummary summary() {
    UUID companyId = companyContext.require();
    BigDecimal totalValue = lots.sumInventoryValue(companyId);
    long active = productRepository.countByCompanyIdAndDeletedAtIsNullAndActiveTrue(companyId);
    long inactive = productRepository.countByCompanyIdAndDeletedAtIsNullAndActiveFalse(companyId);
    long total = active + inactive;
    InventorySettings settings = ensureSettings(companyId);
    BigDecimal threshold = settings.getLowStockThreshold();
    long alerts = lots.countByCompanyIdAndQtyAvailableLessThan(companyId, threshold);
    return new InventorySummary(totalValue, active, inactive, total, alerts, threshold);
  }

  @Transactional(readOnly = true)
  public InventorySettingsResponse getSettings() {
    UUID companyId = companyContext.require();
    InventorySettings settings = ensureSettings(companyId);
    return new InventorySettingsResponse(settings.getLowStockThreshold(), settings.getUpdatedAt());
  }

  @Transactional
  public InventorySettingsResponse updateSettings(InventorySettingsUpdateRequest req) {
    UUID companyId = companyContext.require();
    InventorySettings settings = ensureSettings(companyId);
    settings.setLowStockThreshold(req.lowStockThreshold());
    InventorySettings saved = settingsRepository.save(settings);
    return new InventorySettingsResponse(saved.getLowStockThreshold(), saved.getUpdatedAt());
  }

  @Transactional
  public InventoryAdjustmentResponse adjust(InventoryAdjustmentRequest req) {
    UUID companyId = companyContext.require();
    validateProductBelongsToCompany(req.productId(), companyId);
    boolean increase = isIncrease(req.direction());
    BigDecimal quantity = req.quantity();
    if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Quantity must be greater than zero");
    }
    String reason = req.reason() == null ? "" : req.reason().trim();
    if (increase) {
      handleIncrease(companyId, req, quantity, reason);
    } else {
      handleDecrease(companyId, req, quantity, reason);
    }
    return new InventoryAdjustmentResponse(req.productId(), quantity, increase ? "increase" : "decrease");
  }

  private void handleIncrease(UUID companyId, InventoryAdjustmentRequest req, BigDecimal quantity, String reason) {
    InventoryLot lot;
    if (req.lotId() != null) {
      lot = lots.findById(req.lotId())
        .orElseThrow(() -> new IllegalArgumentException("Lot not found: " + req.lotId()));
      validateLotOwnership(lot, companyId, req.productId());
      lot.setQtyAvailable(lot.getQtyAvailable().add(quantity));
      if (req.unitCost() != null) {
        lot.setCostUnit(req.unitCost());
      }
      if (req.mfgDate() != null) {
        lot.setMfgDate(req.mfgDate());
      }
      if (req.expDate() != null) {
        lot.setExpDate(req.expDate());
      }
    } else {
      lot = new InventoryLot();
      lot.setCompanyId(companyId);
      lot.setProductId(req.productId());
      lot.setPurchaseItemId(null);
      lot.setQtyAvailable(quantity);
      lot.setCostUnit(req.unitCost() != null ? req.unitCost() : BigDecimal.ZERO);
      lot.setMfgDate(req.mfgDate());
      lot.setExpDate(req.expDate());
    }
    lots.save(lot);

    InventoryMovement movement = new InventoryMovement();
    movement.setCompanyId(companyId);
    movement.setProductId(req.productId());
    movement.setLotId(lot.getId());
    movement.setType("MANUAL_IN");
    movement.setQty(quantity);
    movement.setRefType("MANUAL");
    movement.setRefId(lot.getId());
    movement.setNote(reason);
    movements.save(movement);
  }

  private void handleDecrease(UUID companyId, InventoryAdjustmentRequest req, BigDecimal quantity, String reason) {
    BigDecimal remaining = quantity;
    List<InventoryLot> candidates = new ArrayList<>();
    if (req.lotId() != null) {
      InventoryLot lot = lots.findById(req.lotId())
        .orElseThrow(() -> new IllegalArgumentException("Lot not found: " + req.lotId()));
      validateLotOwnership(lot, companyId, req.productId());
      candidates.add(lot);
    } else {
      candidates.addAll(lots.findByCompanyIdAndProductIdOrderByCreatedAtAsc(companyId, req.productId()));
    }

    for (InventoryLot lot : candidates) {
      if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
        break;
      }
      if (lot.getQtyAvailable().compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      BigDecimal take = lot.getQtyAvailable().min(remaining);
      lot.setQtyAvailable(lot.getQtyAvailable().subtract(take));
      lots.save(lot);

      InventoryMovement movement = new InventoryMovement();
      movement.setCompanyId(companyId);
      movement.setProductId(req.productId());
      movement.setLotId(lot.getId());
      movement.setType("MANUAL_OUT");
      movement.setQty(take);
      movement.setRefType("MANUAL");
      movement.setRefId(lot.getId());
      movement.setNote(reason);
      movements.save(movement);

      remaining = remaining.subtract(take);
    }

    if (remaining.compareTo(BigDecimal.ZERO) > 0) {
      throw new IllegalStateException("Insufficient stock to decrease " + quantity + " units for product " + req.productId());
    }
  }

  private void validateProductBelongsToCompany(UUID productId, UUID companyId) {
    boolean exists = productRepository.findByIdAndCompanyId(productId, companyId).isPresent();
    if (!exists) {
      throw new IllegalArgumentException("Product not found for company: " + productId);
    }
  }

  private void validateLotOwnership(InventoryLot lot, UUID companyId, UUID productId) {
    if (!Objects.equals(lot.getCompanyId(), companyId)) {
      throw new IllegalArgumentException("Lot does not belong to company: " + lot.getId());
    }
    if (!Objects.equals(lot.getProductId(), productId)) {
      throw new IllegalArgumentException("Lot " + lot.getId() + " does not match product " + productId);
    }
  }

  private boolean isIncrease(String direction) {
    String normalized = direction == null ? "" : direction.toLowerCase(Locale.ROOT);
    return switch (normalized) {
      case "increase", "in", "add" -> true;
      case "decrease", "out", "remove" -> false;
      default -> throw new IllegalArgumentException("Unsupported direction: " + direction);
    };
  }

  private BigDecimal resolveThreshold(UUID companyId, BigDecimal override) {
    if (override != null && override.compareTo(BigDecimal.ZERO) > 0) {
      return override;
    }
    InventorySettings settings = ensureSettings(companyId);
    BigDecimal threshold = settings.getLowStockThreshold();
    return threshold != null && threshold.compareTo(BigDecimal.ZERO) > 0 ? threshold : FALLBACK_THRESHOLD;
  }

  private InventorySettings ensureSettings(UUID companyId) {
    return settingsRepository.findById(companyId).orElseGet(() -> {
      InventorySettings settings = new InventorySettings();
      settings.setCompanyId(companyId);
      settings.setLowStockThreshold(FALLBACK_THRESHOLD);
      return settingsRepository.save(settings);
    });
  }
}

