# Fix Last 2 Failing Tests - Sprint 6 Final Push

## Context
We have 162/164 tests passing (98.8%). Only 2 tests remain failing. Need to fix them to reach 100%.

## Test 1: TransactionalIntegrationTest.testReferentialIntegrity_CannotSaveOrphanItems()

**File**: `backend/src/test/java/com/datakomerz/pymes/integration/TransactionalIntegrationTest.java`
**Line**: 189
**Status**: FAILING

### Problem
Test expects that saving a `SaleItem` with a non-existent `sale_id` should throw an Exception due to FK constraint violation. However, H2 database does not validate FK constraints during transaction execution, only at commit time.

### Current Implementation
```java
@Test
@Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
void testReferentialIntegrity_CannotSaveOrphanItems() {
    UUID fakeSaleId = UUID.randomUUID();
    
    SaleItem orphanItem = new SaleItem();
    orphanItem.setSaleId(fakeSaleId); // ID that doesn't exist
    orphanItem.setProductId(productId);
    orphanItem.setQty(new BigDecimal("1"));
    orphanItem.setUnitPrice(new BigDecimal("10000"));
    
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    assertThrows(Exception.class, () -> {
        transactionTemplate.execute(status -> {
            saleItemRepository.save(orphanItem);
            return null;
        });
    }, "No debe permitir guardar SaleItem con sale_id inexistente");
}
```

### Database Schema (V1__schema_for_integration.sql)
```sql
CREATE TABLE sale_items (
  id UUID PRIMARY KEY,
  company_id UUID NOT NULL,
  sale_id UUID NOT NULL,
  product_id UUID
);

ALTER TABLE sale_items
  ADD CONSTRAINT fk_sale_items_sale
  FOREIGN KEY (sale_id) REFERENCES sales(id);
```

### Possible Solutions
1. **Option A**: Use `@Sql` annotation to execute raw SQL that will fail immediately
2. **Option B**: Use JDBC directly instead of JPA repository
3. **Option C**: Configure H2 to validate constraints immediately (add to connection URL)
4. **Option D**: Change test to verify constraint exists in schema metadata instead of runtime validation

**Recommended**: Option A or B - most reliable for integration testing.

---

## Test 2: ProductControllerMultipartTest.rejectsUnsupportedImageFormat()

**File**: `backend/src/test/java/com/datakomerz/pymes/products/ProductControllerMultipartTest.java`
**Line**: 96
**Status**: FAILING

### Problem
Test expects that uploading a GIF image should return 400 Bad Request with message containing "Unsupported image format". The controller has validation logic and GlobalExceptionHandler handles `IllegalArgumentException` correctly, but test is still failing.

### Current Implementation
```java
@Test
void rejectsUnsupportedImageFormat() throws Exception {
    when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
        Product product = invocation.getArgument(0);
        if (product.getId() == null) {
            product.setId(UUID.randomUUID());
        }
        product.setCriticalStock(BigDecimal.ZERO);
        return product;
    });

    MockMultipartFile payload = new MockMultipartFile(
        "product",
        "product.json",
        MediaType.APPLICATION_JSON_VALUE,
        "{\"sku\":\"SKU-001\",\"name\":\"Demo\",\"description\":\"Test\",\"category\":\"Cat\",\"barcode\":\"123\"}".getBytes(StandardCharsets.UTF_8)
    );

    MockMultipartFile invalidImage = new MockMultipartFile(
        "image",
        "demo.gif",
        MediaType.IMAGE_GIF_VALUE,
        "gif".getBytes(StandardCharsets.UTF_8)
    );

    mockMvc.perform(multipart("/api/v1/products")
        .file(payload)
        .file(invalidImage)
        .with(jwt().jwt(jwt -> jwt.claim("realm_access", Map.of("roles", List.of("erp_user"))))
            .authorities(new SimpleGrantedAuthority("ROLE_ERP_USER")))
        .header("X-Company-Id", COMPANY_ID))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail", Matchers.containsString("Unsupported image format")));

    verify(storageService, never()).storeProductImage(any(), any(), any());
}
```

### Controller Validation Logic (ProductController.java)
```java
private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
    MediaType.IMAGE_PNG_VALUE,
    MediaType.IMAGE_JPEG_VALUE,
    "image/webp"
);

private void validateImage(MultipartFile image) {
    if (image.getSize() > MAX_IMAGE_BYTES) {
        throw new IllegalArgumentException("Image exceeds maximum allowed size of 1 MB");
    }
    String contentType = image.getContentType();
    if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
        throw new IllegalArgumentException("Unsupported image format. Use PNG, JPEG or WebP");
    }
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
    handleImageUpload(companyId, saved, image, req.imageUrl());  // <-- validateImage() called here
    ensureQr(companyId, saved, true);
    Product persisted = repo.save(saved);
    return toResponse(persisted);
}
```

### GlobalExceptionHandler.java
```java
@ExceptionHandler(IllegalArgumentException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
    var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    pd.setTitle("Bad Request");
    return pd;
}
```

### Possible Issues
1. **Validation not being called**: `handleImageUpload()` might have conditional logic that skips validation when `image` is null or when `imageUrl` is provided
2. **Mock not set up correctly**: `CompanyContext` might not be mocked in test
3. **Security filters**: Test uses `@SpringBootTest` with real security, might be returning 403 instead of 400
4. **Exception not propagating**: Something catching the exception before GlobalExceptionHandler

### Debug Steps
1. Check `handleImageUpload()` implementation to see when `validateImage()` is called
2. Add `.andDo(print())` to see actual response
3. Verify CompanyContext is properly mocked or excluded
4. Check if test needs to mock additional dependencies

---

## Instructions for Codex

Please analyze both tests and provide:

1. **Root cause** for each failure
2. **Minimal code changes** to fix both tests
3. **Explanation** of why the fix works

Focus on making the tests pass WITHOUT changing the production code behavior. The goal is to have valid integration tests that properly verify the intended functionality.

Current test results: 162/164 passing (98.8%)
Target: 164/164 passing (100%)
