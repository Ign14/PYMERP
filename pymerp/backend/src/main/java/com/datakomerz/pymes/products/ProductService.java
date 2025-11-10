package com.datakomerz.pymes.products;

import com.datakomerz.pymes.inventory.InventoryService;
import com.datakomerz.pymes.products.dto.LowStockProduct;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ProductService {

  private final ProductRepository repository;
  private final InventoryService inventoryService;

  public ProductService(ProductRepository repository, InventoryService inventoryService) {
    this.repository = repository;
    this.inventoryService = inventoryService;
  }

  @Cacheable(value = "products", key = "#companyId + ':' + #id")
  @Transactional(Transactional.TxType.SUPPORTS)
  public Optional<Product> findById(UUID companyId, UUID id) {
    return repository.findById(id);
  }

  @Cacheable(value = "products", key = "#companyId + ':all:' + #pageable.pageNumber")
  @Transactional(Transactional.TxType.SUPPORTS)
  public Page<Product> findAll(UUID companyId, Pageable pageable) {
    return repository.findByDeletedAtIsNullAndActiveIsTrue(pageable);
  }

  @CacheEvict(value = "products", allEntries = true)
  public Product save(Product product) {
    return repository.save(product);
  }

  @Caching(evict = {
    @CacheEvict(value = "products", key = "#companyId + ':' + #id"),
    @CacheEvict(value = "products", allEntries = true)
  })
  public void delete(UUID companyId, UUID id) {
    Product entity = repository.findById(id)
      .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
    entity.setDeletedAt(OffsetDateTime.now());
    repository.save(entity);
  }

  @Transactional(Transactional.TxType.SUPPORTS)
  public List<LowStockProduct> findLowStockProducts(UUID companyId) {
    List<Product> activeProducts = repository.findByDeletedAtIsNullAndActiveIsTrue(Pageable.unpaged()).getContent();
    List<LowStockProduct> lowStockProducts = new ArrayList<>();
    
    for (Product product : activeProducts) {
      BigDecimal currentStock = inventoryService.getTotalStock(product.getId());
      BigDecimal criticalStock = product.getCriticalStock();
      
      if (criticalStock != null && criticalStock.compareTo(BigDecimal.ZERO) > 0 
          && currentStock.compareTo(criticalStock) < 0) {
        BigDecimal deficit = criticalStock.subtract(currentStock);
        lowStockProducts.add(new LowStockProduct(
          product.getId(),
          product.getSku(),
          product.getName(),
          product.getCategory(),
          currentStock,
          criticalStock,
          deficit
        ));
      }
    }
    
    return lowStockProducts;
  }
}
