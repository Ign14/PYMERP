package com.datakomerz.pymes.products;

import com.datakomerz.pymes.core.tenancy.CompanyContext;
import com.datakomerz.pymes.inventory.InventoryLot;
import com.datakomerz.pymes.inventory.InventoryService;
import com.datakomerz.pymes.pricing.PricingService;
import com.datakomerz.pymes.products.dto.ProductInventoryAlertRequest;
import com.datakomerz.pymes.products.dto.ProductReq;
import com.datakomerz.pymes.products.dto.ProductRes;
import com.datakomerz.pymes.products.dto.ProductStatusRequest;
import com.datakomerz.pymes.products.dto.ProductStockLot;
import com.datakomerz.pymes.products.dto.ProductStockResponse;
import com.datakomerz.pymes.multitenancy.ValidateTenant;
import com.datakomerz.pymes.storage.StorageService;
import com.datakomerz.pymes.storage.StorageService.StoredFile;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
  private final ProductRepository repo;
  private final CompanyContext companyContext;
  private final PricingService pricingService;
  private final StorageService storageService;
  private final QrCodeService qrCodeService;
  private final InventoryService inventoryService;

  private static final long MAX_IMAGE_BYTES = 1_048_576; // 1 MB
  private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
    MediaType.IMAGE_PNG_VALUE,
    MediaType.IMAGE_JPEG_VALUE,
    "image/webp"
  );

  public ProductController(ProductRepository repo,
                           CompanyContext companyContext,
                           PricingService pricingService,
                           StorageService storageService,
                           QrCodeService qrCodeService,
                           InventoryService inventoryService) {
    this.repo = repo;
    this.companyContext = companyContext;
    this.pricingService = pricingService;
    this.storageService = storageService;
    this.qrCodeService = qrCodeService;
    this.inventoryService = inventoryService;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN') or hasAuthority('SCOPE_products:read')")
  public Page<ProductRes> list(@RequestParam(defaultValue = "") String q,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size,
                               @RequestParam(defaultValue = "active") String status) {
    companyContext.require();
    Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
    Boolean activeFilter = resolveActiveFilter(status);
    Page<Product> products;
    String query = (q == null) ? "" : q.trim();
    if (query.isEmpty()) {
      products = findByName("", pageable, activeFilter);
    } else if (query.matches("^\\d+$")) {
      products = findByBarcode(query, pageable, activeFilter);
    } else if (query.toUpperCase().startsWith("SKU-")) {
      products = findBySku(query, pageable, activeFilter);
    } else {
      products = findByName(query, pageable, activeFilter);
    }
    return products.map(this::toResponse);
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
  public ProductRes create(@Valid @RequestPart("product") ProductReq req,
                           @RequestPart(value = "image", required = false) MultipartFile image) throws IOException {
    UUID companyId = companyContext.require();
    Product entity = new Product();
    entity.setCompanyId(companyId);
    entity.setActive(Boolean.TRUE);
    apply(entity, req);
    if (entity.getCriticalStock() == null) {
      entity.setCriticalStock(BigDecimal.ZERO);
    }
    Product saved = repo.save(entity);
    handleImageUpload(companyId, saved, image, req.imageUrl());
    ensureQr(companyId, saved, true);
    Product persisted = repo.save(saved);
    return toResponse(persisted);
  }

  @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
  @ValidateTenant(entityClass = Product.class, entityParamIndex = 0)
  public ProductRes update(@PathVariable UUID id,
                           @Valid @RequestPart("product") ProductReq req,
                           @RequestPart(value = "image", required = false) MultipartFile image) throws IOException {
    UUID companyId = companyContext.require();
    Product entity = repo.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    String previousSku = entity.getSku();
    apply(entity, req);
    Product saved = repo.save(entity);
    handleImageUpload(companyId, saved, image, req.imageUrl());
    boolean skuChanged = previousSku != null && !previousSku.equals(saved.getSku());
    ensureQr(companyId, saved, skuChanged);
    Product persisted = repo.save(saved);
    return toResponse(persisted);
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
  @ValidateTenant(entityClass = Product.class, entityParamIndex = 0)
  public ProductRes updateStatus(@PathVariable UUID id, @Valid @RequestBody ProductStatusRequest req) {
    companyContext.require();
    Product entity = repo.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    entity.setActive(req.active());
    Product saved = repo.save(entity);
    return toResponse(saved);
  }

  @PatchMapping("/{id}/inventory-alert")
  @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
  @ValidateTenant(entityClass = Product.class, entityParamIndex = 0)
  public ProductRes updateInventoryAlert(@PathVariable UUID id,
                                         @Valid @RequestBody ProductInventoryAlertRequest req) {
    companyContext.require();
    Product entity = repo.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    entity.setCriticalStock(req.criticalStock());
    Product saved = repo.save(entity);
    return toResponse(saved);
  }

  @GetMapping("/{id}/qr")
  @ValidateTenant(entityClass = Product.class, entityParamIndex = 0)
  public ResponseEntity<Resource> getQr(@PathVariable UUID id,
                                        @RequestParam(name = "download", defaultValue = "false") boolean download) throws IOException {
    UUID companyId = companyContext.require();
    Product entity = repo.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    boolean regenerated = ensureQr(companyId, entity, entity.getQrUrl() == null || entity.getQrUrl().isBlank());
    if (regenerated) {
      repo.save(entity);
    }
    if (entity.getQrUrl() == null || entity.getQrUrl().isBlank()) {
      throw new EntityNotFoundException("QR code not available for product: " + id);
    }
    StoredFile file;
    try {
      file = storageService.load(entity.getQrUrl());
    } catch (IOException ex) {
      boolean regeneratedNow = ensureQr(companyId, entity, true);
      if (regeneratedNow) {
        repo.save(entity);
      }
      file = storageService.load(entity.getQrUrl());
    }
    MediaType mediaType = file.contentType() != null
      ? MediaType.parseMediaType(file.contentType())
      : MediaType.APPLICATION_OCTET_STREAM;
    ResponseEntity.BodyBuilder builder = ResponseEntity.ok().contentType(mediaType);
    if (download) {
      String filename = buildDownloadFilename(entity.getSku(), file.filename());
      builder.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString());
    }
    return builder.body(file.resource());
  }

  @GetMapping("/{id}/stock")
  @ValidateTenant(entityClass = Product.class, entityParamIndex = 0)
  public ProductStockResponse stock(@PathVariable UUID id) {
    companyContext.require();
    Product entity = repo.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    List<InventoryLot> lots = inventoryService.lotsForProduct(entity.getId());
    BigDecimal total = BigDecimal.ZERO;
    List<ProductStockLot> items = new ArrayList<>();
    for (InventoryLot lot : lots) {
      total = total.add(lot.getQtyAvailable());
      items.add(new ProductStockLot(
        lot.getId(),
        lot.getQtyAvailable(),
        null,
        lot.getExpDate()
      ));
    }
    return new ProductStockResponse(entity.getId(), total, items);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ValidateTenant(entityClass = Product.class, entityParamIndex = 0)
  public void delete(@PathVariable UUID id) {
    companyContext.require();
    Product entity = repo.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    entity.setDeletedAt(OffsetDateTime.now());
    repo.save(entity);
  }

  private Page<Product> findByName(String query, Pageable pageable, Boolean activeFilter) {
    if (activeFilter == null) {
      return repo.findByDeletedAtIsNullAndNameContainingIgnoreCase(query, pageable);
    }
    return repo.findByDeletedAtIsNullAndActiveIsAndNameContainingIgnoreCase(activeFilter, query, pageable);
  }

  private Page<Product> findBySku(String query, Pageable pageable, Boolean activeFilter) {
    if (activeFilter == null) {
      return repo.findByDeletedAtIsNullAndSkuContainingIgnoreCase(query, pageable);
    }
    return repo.findByDeletedAtIsNullAndActiveIsAndSkuContainingIgnoreCase(activeFilter, query, pageable);
  }

  private Page<Product> findByBarcode(String query, Pageable pageable, Boolean activeFilter) {
    if (activeFilter == null) {
      return repo.findByDeletedAtIsNullAndBarcode(query, pageable);
    }
    return repo.findByDeletedAtIsNullAndActiveIsAndBarcode(activeFilter, query, pageable);
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
    entity.setDescription(trimToNull(req.description()));
    entity.setCategory(trimToNull(req.category()));
    entity.setBarcode(trimToNull(req.barcode()));
    if (req.imageUrl() != null) {
      entity.setImageUrl(trimToNull(req.imageUrl()));
    }
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
      entity.getQrUrl(),
      entity.getCriticalStock() != null ? entity.getCriticalStock() : BigDecimal.ZERO,
      price,
      active
    );
  }

  private void handleImageUpload(UUID companyId, Product product, MultipartFile image, String requestImageUrl) throws IOException {
    if (image != null && !image.isEmpty()) {
      validateImage(image);
      String url = storageService.storeProductImage(companyId, product.getId(), image);
      product.setImageUrl(url);
    } else if (requestImageUrl != null) {
      product.setImageUrl(trimToNull(requestImageUrl));
    }
  }

  private boolean ensureQr(UUID companyId, Product product, boolean force) throws IOException {
    boolean needsGeneration = force || product.getQrUrl() == null || product.getQrUrl().isBlank();
    if (!needsGeneration) {
      return false;
    }
    QrCodeService.GeneratedQr qr = qrCodeService.generate(product.getSku());
    String url = storageService.storeProductQr(companyId, product.getId(), qr.content(), qr.extension());
    product.setQrUrl(url);
    return true;
  }

  private void validateImage(MultipartFile image) {
    if (image.getSize() > MAX_IMAGE_BYTES) {
      throw new IllegalArgumentException("Image exceeds maximum allowed size of 1 MB");
    }
    String contentType = image.getContentType();
    if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
      throw new IllegalArgumentException("Unsupported image format. Use PNG, JPEG or WebP");
    }
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private String buildDownloadFilename(String sku, String originalFilename) {
    String base = trimToNull(sku);
    if (base == null) {
      base = "product-qr";
    }
    String safeBase = base
      .toLowerCase(Locale.ROOT)
      .replaceAll("[^a-z0-9-_]", "-");
    if (safeBase.isBlank()) {
      safeBase = "product-qr";
    }
    String extension = "";
    if (originalFilename != null) {
      int dot = originalFilename.lastIndexOf('.');
      if (dot >= 0) {
        extension = originalFilename.substring(dot);
      }
    }
    if (extension.isBlank()) {
      extension = ".png";
    }
    return safeBase + extension;
  }
}
