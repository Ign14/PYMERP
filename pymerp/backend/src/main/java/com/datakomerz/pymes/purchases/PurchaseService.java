package com.datakomerz.pymes.purchases;

import com.datakomerz.pymes.core.tenancy.CompanyContext;
import com.datakomerz.pymes.inventory.InventoryLot;
import com.datakomerz.pymes.inventory.InventoryLotRepository;
import com.datakomerz.pymes.inventory.InventoryMovement;
import com.datakomerz.pymes.inventory.InventoryMovementRepository;
import com.datakomerz.pymes.purchases.dto.PurchaseDailyPoint;
import com.datakomerz.pymes.purchases.dto.PurchaseReq;
import com.datakomerz.pymes.purchases.dto.PurchaseSummary;
import com.datakomerz.pymes.purchases.dto.PurchaseUpdateRequest;
import com.datakomerz.pymes.suppliers.Supplier;
import com.datakomerz.pymes.suppliers.SupplierRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseService {
  private final PurchaseRepository purchases;
  private final PurchaseItemRepository items;
  private final InventoryLotRepository lots;
  private final InventoryMovementRepository movements;
  private final CompanyContext companyContext;
  private final SupplierRepository suppliers;

  public PurchaseService(PurchaseRepository purchases,
                         PurchaseItemRepository items,
                         InventoryLotRepository lots,
                         InventoryMovementRepository movements,
                         CompanyContext companyContext,
                         SupplierRepository suppliers) {
    this.purchases = purchases;
    this.items = items;
    this.lots = lots;
    this.movements = movements;
    this.companyContext = companyContext;
    this.suppliers = suppliers;
  }

  @Transactional
  public UUID create(PurchaseReq req) {
    UUID companyId = companyContext.require();

    var purchase = new Purchase();
    purchase.setCompanyId(companyId);
    purchase.setSupplierId(req.supplierId());
    purchase.setDocType(req.docType());
    purchase.setDocNumber(req.docNumber());
    purchase.setStatus("received");
    purchase.setNet(req.net());
    purchase.setVat(req.vat());
    purchase.setTotal(req.total());
    purchase.setPdfUrl(req.pdfUrl());
    purchase.setIssuedAt(req.issuedAt());
    purchases.save(purchase);

    for (var itemReq : req.items()) {
      var item = new PurchaseItem();
      item.setPurchaseId(purchase.getId());
      item.setProductId(itemReq.productId());
      item.setQty(itemReq.qty());
      item.setUnitCost(itemReq.unitCost());
      item.setVatRate(itemReq.vatRate());
      item.setMfgDate(itemReq.mfgDate());
      item.setExpDate(itemReq.expDate());
      items.save(item);

      var lot = new InventoryLot();
      lot.setCompanyId(companyId);
      lot.setProductId(itemReq.productId());
      lot.setPurchaseItemId(item.getId());
      lot.setQtyAvailable(itemReq.qty());
      lot.setCostUnit(itemReq.unitCost());
      lot.setMfgDate(itemReq.mfgDate());
      lot.setExpDate(itemReq.expDate());
      lots.save(lot);

      var movement = new InventoryMovement();
      movement.setCompanyId(companyId);
      movement.setProductId(itemReq.productId());
      movement.setLotId(lot.getId());
      movement.setType("PURCHASE_IN");
      movement.setQty(itemReq.qty());
      movement.setRefType("PURCHASE");
      movement.setRefId(purchase.getId());
      movements.save(movement);
    }
    return purchase.getId();
  }

  @Transactional(readOnly = true)
  public Page<PurchaseSummary> list(String status,
                                    String docType,
                                    String search,
                                    OffsetDateTime from,
                                    OffsetDateTime to,
                                    Pageable pageable) {
    UUID companyId = companyContext.require();
    Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("issuedAt").descending());
    Page<Purchase> page = purchases.search(companyId, emptyToNull(status), emptyToNull(docType), emptyToNull(search), from, to, sorted);
    Map<UUID, String> supplierNames = resolveSupplierNames(companyId, page.getContent());
    return page.map(purchase -> mapToSummary(purchase, supplierNames.get(purchase.getSupplierId())));
  }

  @Transactional
  public PurchaseSummary update(UUID id, PurchaseUpdateRequest req) {
    UUID companyId = companyContext.require();
    Purchase purchase = purchases.findByIdAndCompanyId(id, companyId)
      .orElseThrow(() -> new IllegalStateException("Purchase not found: " + id));

    if (req == null) {
      String supplierName = resolveSupplierName(companyId, purchase.getSupplierId());
      return mapToSummary(purchase, supplierName);
    }

    if (req.status() != null && !req.status().equalsIgnoreCase(purchase.getStatus())) {
      if ("cancelled".equalsIgnoreCase(req.status())) {
        return cancel(id);
      }
      purchase.setStatus(req.status());
    }

    if (req.docType() != null) {
      purchase.setDocType(req.docType());
    }
    if (req.docNumber() != null) {
      purchase.setDocNumber(req.docNumber());
    }

    purchases.save(purchase);
    String supplierName = resolveSupplierName(companyId, purchase.getSupplierId());
    return mapToSummary(purchase, supplierName);
  }

  @Transactional
  public PurchaseSummary cancel(UUID id) {
    UUID companyId = companyContext.require();
    Purchase purchase = purchases.findByIdAndCompanyId(id, companyId)
      .orElseThrow(() -> new IllegalStateException("Purchase not found: " + id));

    if ("cancelled".equalsIgnoreCase(purchase.getStatus())) {
      String supplierName = resolveSupplierName(companyId, purchase.getSupplierId());
      return mapToSummary(purchase, supplierName);
    }

    List<PurchaseItem> purchaseItems = items.findByPurchaseId(purchase.getId());
    for (PurchaseItem item : purchaseItems) {
      List<InventoryLot> relatedLots = lots.findByPurchaseItemId(item.getId());
      for (InventoryLot lot : relatedLots) {
        BigDecimal qty = item.getQty();
        if (lot.getQtyAvailable().compareTo(qty) < 0) {
          throw new IllegalStateException("Cannot cancel purchase because stock was already consumed");
        }
        lot.setQtyAvailable(lot.getQtyAvailable().subtract(qty));
        lots.save(lot);

        InventoryMovement movement = new InventoryMovement();
        movement.setCompanyId(companyId);
        movement.setProductId(item.getProductId());
        movement.setLotId(lot.getId());
        movement.setType("PURCHASE_CANCEL");
        movement.setQty(qty.negate());
        movement.setRefType("PURCHASE");
        movement.setRefId(purchase.getId());
        movements.save(movement);
      }
    }

    purchase.setStatus("cancelled");
    purchases.save(purchase);
    String supplierName = resolveSupplierName(companyId, purchase.getSupplierId());
    return mapToSummary(purchase, supplierName);
  }

  @Transactional(readOnly = true)
  public List<PurchaseDailyPoint> dailyMetrics(int days) {
    UUID companyId = companyContext.require();
    OffsetDateTime from = OffsetDateTime.now().minusDays(days);
    List<Purchase> range = purchases.findByCompanyIdAndIssuedAtGreaterThanEqualOrderByIssuedAtAsc(companyId, from);
    return range.stream()
      .collect(Collectors.groupingBy(p -> p.getIssuedAt().toLocalDate()))
      .entrySet().stream()
      .map(entry -> {
        BigDecimal total = entry.getValue().stream()
          .map(Purchase::getTotal)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
        long count = entry.getValue().size();
        return new PurchaseDailyPoint(entry.getKey(), total, count);
      })
      .sorted(java.util.Comparator.comparing(PurchaseDailyPoint::date))
      .toList();
  }

  private PurchaseSummary mapToSummary(Purchase purchase, String supplierName) {
    return new PurchaseSummary(
      purchase.getId(),
      purchase.getSupplierId(),
      supplierName,
      purchase.getDocType(),
      purchase.getDocNumber(),
      purchase.getStatus(),
      purchase.getNet(),
      purchase.getVat(),
      purchase.getTotal(),
      purchase.getIssuedAt()
    );
  }

  private Map<UUID, String> resolveSupplierNames(UUID companyId, List<Purchase> purchaseList) {
    Set<UUID> ids = purchaseList.stream()
      .map(Purchase::getSupplierId)
      .filter(Objects::nonNull)
      .collect(Collectors.toCollection(HashSet::new));
    if (ids.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<UUID, String> map = new HashMap<>();
    suppliers.findAllById(ids).forEach(supplier -> {
      if (companyId.equals(supplier.getCompanyId())) {
        map.put(supplier.getId(), supplier.getName());
      }
    });
    return map;
  }

  private String resolveSupplierName(UUID companyId, UUID supplierId) {
    if (supplierId == null) {
      return null;
    }
    Optional<Supplier> supplier = suppliers.findById(supplierId);
    return supplier.filter(s -> companyId.equals(s.getCompanyId()))
      .map(Supplier::getName)
      .orElse(null);
  }

  private String emptyToNull(String value) {
    return (value != null && !value.isBlank()) ? value : null;
  }
}
