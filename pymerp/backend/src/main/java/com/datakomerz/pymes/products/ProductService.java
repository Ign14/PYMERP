package com.datakomerz.pymes.products;

import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
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

  public ProductService(ProductRepository repository) {
    this.repository = repository;
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
}
