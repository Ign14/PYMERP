package com.datakomerz.pymes.pricing;

import com.datakomerz.pymes.core.tenancy.CompanyContext;
import com.datakomerz.pymes.pricing.dto.PriceChangeRequest;
import com.datakomerz.pymes.products.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class PricingService {

  private final PriceHistoryRepository priceHistoryRepository;
  private final ProductRepository productRepository;
  private final CompanyContext companyContext;

  public PricingService(PriceHistoryRepository priceHistoryRepository,
                        ProductRepository productRepository,
                        CompanyContext companyContext) {
    this.priceHistoryRepository = priceHistoryRepository;
    this.productRepository = productRepository;
    this.companyContext = companyContext;
  }

  @Transactional(Transactional.TxType.SUPPORTS)
  public Page<PriceHistory> history(UUID productId, Pageable pageable) {
    ensureOwnership(productId);
    return priceHistoryRepository.findByProductIdOrderByValidFromDesc(productId, pageable);
  }

  public PriceHistory registerPrice(UUID productId, PriceChangeRequest request) {
    var product = ensureOwnership(productId);
    PriceHistory entry = new PriceHistory();
    entry.setProductId(product.getId());
    entry.setPrice(request.price().setScale(4, RoundingMode.HALF_UP));
    entry.setValidFrom(request.validFrom() != null ? request.validFrom() : OffsetDateTime.now());
    return priceHistoryRepository.save(entry);
  }

  @Transactional(Transactional.TxType.SUPPORTS)
  public Optional<BigDecimal> latestPrice(UUID productId) {
    return priceHistoryRepository.findFirstByProductIdOrderByValidFromDesc(productId)
      .map(PriceHistory::getPrice);
  }

  private com.datakomerz.pymes.products.Product ensureOwnership(UUID productId) {
    UUID companyId = companyContext.require();
    return productRepository.findByIdAndCompanyId(productId, companyId)
      .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
  }
}
