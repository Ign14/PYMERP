package com.datakomerz.pymes.products;

import com.datakomerz.pymes.core.tenancy.CompanyContext;
import com.datakomerz.pymes.inventory.InventoryLot;
import com.datakomerz.pymes.inventory.InventoryService;
import com.datakomerz.pymes.multitenancy.ValidateTenant;
import com.datakomerz.pymes.pricing.PricingService;
import com.datakomerz.pymes.products.dto.ProductInventoryAlertRequest;
import com.datakomerz.pymes.products.dto.ProductReq;
import com.datakomerz.pymes.products.dto.ProductRes;
import com.datakomerz.pymes.products.dto.ProductStatusRequest;
import com.datakomerz.pymes.products.dto.ProductStockLot;
import com.datakomerz.pymes.products.dto.ProductStockResponse;
import com.datakomerz.pymes.storage.StorageService;
import com.datakomerz.pymes.storage.StorageService.StoredFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
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

@Tag(name = "Products", description = "Gestión de productos del catálogo")
@RestController
@RequestMapping("/api/v1/products")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {
  private final ProductRepository repo;
  private final CompanyContext companyContext;
  private final PricingService pricingService;
  private final StorageService storageService;
  private final QrCodeService qrCodeService;
  private final InventoryService inventoryService;
  private final ProductService productService;

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
                           InventoryService inventoryService,
                           ProductService productService) {
    this.repo = repo;
    this.companyContext = companyContext;
    this.pricingService = pricingService;
    this.storageService = storageService;
    this.qrCodeService = qrCodeService;
    this.inventoryService = inventoryService;
    this.productService = productService;
  }

  @Operation(
    summary = "Listar productos",
    description = "Retorna página de productos filtrados por estado, texto de búsqueda o código de barras."
  )
  @ApiResponses({
    @ApiResponse(
      responseCode = "200",
      description = "Lista de productos",
      content = @Content(schema = @Schema(implementation = Page.class))
    ),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN') or hasAuthority('SCOPE_products:read')")
  public Page<ProductRes> list(
      @Parameter(description = "Texto de búsqueda (nombre, SKU o código de barras)", example = "SKU-001")
      @RequestParam(defaultValue = "") String q,
      @Parameter(description = "Número de página (0-indexado)", example = "0")
      @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Cantidad de elementos por página", example = "20")
      @RequestParam(defaultValue = "20") int size,
      @Parameter(description = "Filtro de estado: active, inactive o all", example = "active")
      @RequestParam(defaultValue = "active") String status) {
    UUID companyId = companyContext.require();
    Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
    Boolean activeFilter = resolveActiveFilter(status);
    Page<Product> products;
    String query = (q == null) ? "" : q.trim();
    if (query.isEmpty() && Boolean.TRUE.equals(activeFilter)) {
      products = productService.findAll(companyId, pageable);
    } else if (query.isEmpty()) {
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

  @Operation(
    summary = "Crear producto",
    description = "Registra un nuevo producto del catálogo con su imagen opcional y configuración de stock."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Producto creado", content = @Content(schema = @Schema(implementation = ProductRes.class))),
    @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
  public ProductRes create(
      @Parameter(description = "Datos del producto a registrar", required = true)
      @Valid @RequestPart("product") ProductReq req,
      @Parameter(
        description = "Imagen del producto (PNG, JPG o WebP, máximo 1 MB)",
        content = @Content(
          mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
          schema = @Schema(type = "string", format = "binary")
        )
      )
      @RequestPart(value = "image", required = false) MultipartFile image) throws IOException {
    UUID companyId = companyContext.require();
    Product entity = new Product();
    entity.setCompanyId(companyId);
    entity.setActive(Boolean.TRUE);
    apply(entity, req);
    if (entity.getCriticalStock() == null) {
      entity.setCriticalStock(BigDecimal.ZERO);
    }
    Product saved = productService.save(entity);
    handleImageUpload(companyId, saved, image, req.imageUrl());
    ensureQr(companyId, saved, true);
    Product persisted = productService.save(saved);
    return toResponse(persisted);
  }

  @Operation(
    summary = "Actualizar producto",
    description = "Modifica los datos generales, imagen o códigos de un producto existente."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Producto actualizado", content = @Content(schema = @Schema(implementation = ProductRes.class))),
    @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos"),
    @ApiResponse(responseCode = "404", description = "Producto no encontrado")
  })
  @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
  @ValidateTenant(entityClass = Product.class, entityParamIndex = 0)
  public ProductRes update(
      @Parameter(description = "ID del producto a actualizar", required = true)
      @PathVariable UUID id,
      @Parameter(description = "Datos del producto a actualizar", required = true)
      @Valid @RequestPart("product") ProductReq req,
      @Parameter(
        description = "Nueva imagen del producto (opcional)",
        content = @Content(
          mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
          schema = @Schema(type = "string", format = "binary")
        )
      )
      @RequestPart(value = "image", required = false) MultipartFile image) throws IOException {
    UUID companyId = companyContext.require();
    Product entity = productService.findById(companyId, id)
      .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    String previousSku = entity.getSku();
    apply(entity, req);
    Product saved = productService.save(entity);
    handleImageUpload(companyId, saved, image, req.imageUrl());
    boolean skuChanged = previousSku != null && !previousSku.equals(saved.getSku());
    ensureQr(companyId, saved, skuChanged);
    Product persisted = productService.save(saved);
    return toResponse(persisted);
  }

  @Operation(
    summary = "Actualizar estado de producto",
    description = "Activa o desactiva un producto sin modificar el resto de su configuración."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Estado actualizado", content = @Content(schema = @Schema(implementation = ProductRes.class))),
    @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos"),
    @ApiResponse(responseCode = "404", description = "Producto no encontrado")
  })
  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
  @ValidateTenant(entityClass = Product.class, entityParamIndex = 0)
  public ProductRes updateStatus(
      @Parameter(description = "ID del producto a modificar", required = true)
      @PathVariable UUID id,
      @Valid @RequestBody ProductStatusRequest req) {
    UUID companyId = companyContext.require();
    Product entity = productService.findById(companyId, id)
      .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    entity.setActive(req.active());
    Product saved = productService.save(entity);
    return toResponse(saved);
  }

  @Operation(
    summary = "Configurar alerta de inventario",
    description = "Actualiza el stock crítico que dispara alertas y reposiciones."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Alerta configurada", content = @Content(schema = @Schema(implementation = ProductRes.class))),
    @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos"),
    @ApiResponse(responseCode = "404", description = "Producto no encontrado")
  })
  @PatchMapping("/{id}/inventory-alert")
  @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
  @ValidateTenant(entityClass = Product.class, entityParamIndex = 0)
  public ProductRes updateInventoryAlert(
      @Parameter(description = "ID del producto a configurar", required = true)
      @PathVariable UUID id,
      @Valid @RequestBody ProductInventoryAlertRequest req) {
    UUID companyId = companyContext.require();
    Product entity = productService.findById(companyId, id)
      .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    entity.setCriticalStock(req.criticalStock());
    Product saved = productService.save(entity);
    return toResponse(saved);
  }

  @Operation(
    summary = "Descargar código QR",
    description = "Obtiene o regenera el código QR asociado al producto para impresión o descarga."
  )
  @ApiResponses({
    @ApiResponse(
      responseCode = "200",
      description = "QR disponible",
      content = @Content(mediaType = "image/png")
    ),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos"),
    @ApiResponse(responseCode = "404", description = "Producto o QR no disponible")
  })
  @GetMapping("/{id}/qr")
  @ValidateTenant(entityClass = Product.class, entityParamIndex = 0)
  public ResponseEntity<Resource> getQr(
      @Parameter(description = "ID del producto", required = true)
      @PathVariable UUID id,
      @Parameter(description = "Forzar descarga como archivo adjunto", example = "false")
      @RequestParam(name = "download", defaultValue = "false") boolean download) throws IOException {
    UUID companyId = companyContext.require();
    Product entity = productService.findById(companyId, id)
      .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    boolean regenerated = ensureQr(companyId, entity, entity.getQrUrl() == null || entity.getQrUrl().isBlank());
    if (regenerated) {
      productService.save(entity);
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
        productService.save(entity);
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

  @Operation(
    summary = "Consultar stock del producto",
    description = "Devuelve el stock total y el detalle por lotes del producto."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Stock recuperado", content = @Content(schema = @Schema(implementation = ProductStockResponse.class))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos"),
    @ApiResponse(responseCode = "404", description = "Producto no encontrado")
  })
  @GetMapping("/{id}/stock")
  @ValidateTenant(entityClass = Product.class, entityParamIndex = 0)
  public ProductStockResponse stock(
      @Parameter(description = "ID del producto a consultar", required = true)
      @PathVariable UUID id) {
    UUID companyId = companyContext.require();
    Product entity = productService.findById(companyId, id)
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

  @Operation(
    summary = "Eliminar producto",
    description = "Elimina lógicamente un producto del catálogo de la empresa."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Producto eliminado"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos"),
    @ApiResponse(responseCode = "404", description = "Producto no encontrado")
  })
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN')")
  @ValidateTenant(entityClass = Product.class, entityParamIndex = 0)
  public void delete(
      @Parameter(description = "ID del producto a eliminar", required = true)
      @PathVariable UUID id) {
    UUID companyId = companyContext.require();
    productService.delete(companyId, id);
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
