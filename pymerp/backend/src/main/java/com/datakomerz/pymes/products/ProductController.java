package com.datakomerz.pymes.products;

import com.datakomerz.pymes.core.tenancy.CompanyContext;
import com.datakomerz.pymes.pricing.PricingService;
import com.datakomerz.pymes.products.dto.ProductReq;
import com.datakomerz.pymes.products.dto.ProductRes;
import com.datakomerz.pymes.products.dto.ProductStatusRequest;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
  private final ProductRepository repo;
  private final CompanyContext companyContext;
  private final PricingService pricingService;

  public ProductController(ProductRepository repo, CompanyContext companyContext, PricingService pricingService) {
    this.repo = repo;
    this.companyContext = companyContext;
    this.pricingService = pricingService;
  }

  @GetMapping
  @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ROLE_ERP_USER') or hasAuthority('SCOPE_products:read')")
  public Page<ProductRes> list(@RequestParam(defaultValue = "") String q,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size,
                               @RequestParam(defaultValue = "active") String status) {
    UUID companyId = companyContext.require();
    Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
    Boolean activeFilter = resolveActiveFilter(status);
    Page<Product> products;
    String query = (q == null) ? "" : q.trim();
    if (query.isEmpty()) {
      products = findByName(companyId, "", pageable, activeFilter);
    } else if (query.matches("^\\d+$")) {
      products = findByBarcode(companyId, query, pageable, activeFilter);
    } else if (query.toUpperCase().startsWith("SKU-")) {
      products = findBySku(companyId, query, pageable, activeFilter);
    } else {
      products = findByName(companyId, query, pageable, activeFilter);
    }
    return products.map(this::toResponse);
  }

  @PostMapping
  public ProductRes create(@Valid @RequestBody ProductReq req) {
    UUID companyId = companyContext.require();
    Product entity = new Product();
    entity.setCompanyId(companyId);
    entity.setActive(Boolean.TRUE);
    apply(entity, req);
    Product saved = repo.save(entity);
    return toResponse(saved);
  }

  @PutMapping("/{id}")
  public ProductRes update(@PathVariable UUID id, @Valid @RequestBody ProductReq req) {
    UUID companyId = companyContext.require();
    Product entity = repo.findByIdAndCompanyId(id, companyId)
      .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    apply(entity, req);
    Product saved = repo.save(entity);
    return toResponse(saved);
  }

  @PatchMapping("/{id}/status")
  public ProductRes updateStatus(@PathVariable UUID id, @Valid @RequestBody ProductStatusRequest req) {
    UUID companyId = companyContext.require();
    Product entity = repo.findByIdAndCompanyId(id, companyId)
      .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    entity.setActive(req.active());
    Product saved = repo.save(entity);
    return toResponse(saved);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    UUID companyId = companyContext.require();
    Product entity = repo.findByIdAndCompanyId(id, companyId)
      .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    entity.setDeletedAt(OffsetDateTime.now());
    repo.save(entity);
  }

  private Page<Product> findByName(UUID companyId, String query, Pageable pageable, Boolean activeFilter) {
    if (activeFilter == null) {
      return repo.findByCompanyIdAndDeletedAtIsNullAndNameContainingIgnoreCase(companyId, query, pageable);
    }
    return repo.findByCompanyIdAndDeletedAtIsNullAndActiveIsAndNameContainingIgnoreCase(companyId, activeFilter, query, pageable);
  }

  private Page<Product> findBySku(UUID companyId, String query, Pageable pageable, Boolean activeFilter) {
    if (activeFilter == null) {
      return repo.findByCompanyIdAndDeletedAtIsNullAndSkuContainingIgnoreCase(companyId, query, pageable);
    }
    return repo.findByCompanyIdAndDeletedAtIsNullAndActiveIsAndSkuContainingIgnoreCase(companyId, activeFilter, query, pageable);
  }

  private Page<Product> findByBarcode(UUID companyId, String query, Pageable pageable, Boolean activeFilter) {
    if (activeFilter == null) {
      return repo.findByCompanyIdAndDeletedAtIsNullAndBarcode(companyId, query, pageable);
    }
    return repo.findByCompanyIdAndDeletedAtIsNullAndActiveIsAndBarcode(companyId, activeFilter, query, pageable);
  }

  private Boolean resolveActiveFilter(String status) {
    if (status == null || status.isBlank()) {
      return Boolean.TRUE;
    }
    String value = status.trim().toLowerCase();
    return switch (value) {
      case "active", "true", "1" -> Boolean.TRUE;
      case "inactive", "false", "0" -> Boolean.FALSE;
      case "all" -> null;
      default -> Boolean.TRUE;
    };
  }

  private void apply(Product entity, ProductReq req) {
    entity.setSku(req.sku().trim());
    entity.setName(req.name().trim());
    entity.setDescription(req.description());
    entity.setCategory(req.category());
    entity.setBarcode(req.barcode());
    entity.setImageUrl(req.imageUrl());
  }

  private ProductRes toResponse(Product entity) {
    BigDecimal price = pricingService.latestPrice(entity.getId()).orElse(null);
    boolean active = Boolean.TRUE.equals(entity.getActive());
    return new ProductRes(
      entity.getId(),
      entity.getSku(),
      entity.getName(),
      entity.getDescription(),
      entity.getCategory(),
      entity.getBarcode(),
      entity.getImageUrl(),
      price,
      active
    );
  }
}
