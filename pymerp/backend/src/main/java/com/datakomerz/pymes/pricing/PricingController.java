package com.datakomerz.pymes.pricing;

import com.datakomerz.pymes.pricing.dto.PriceChangeRequest;
import com.datakomerz.pymes.pricing.dto.PriceHistoryResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products/{productId}/prices")
public class PricingController {

  private final PricingService pricingService;

  public PricingController(PricingService pricingService) {
    this.pricingService = pricingService;
  }

  @GetMapping
  public Page<PriceHistoryResponse> list(@PathVariable UUID productId,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("validFrom").descending());
    return pricingService.history(productId, pageable).map(this::toResponse);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PriceHistoryResponse create(@PathVariable UUID productId,
                                     @Valid @RequestBody PriceChangeRequest request) {
    return toResponse(pricingService.registerPrice(productId, request));
  }

  private PriceHistoryResponse toResponse(PriceHistory history) {
    return new PriceHistoryResponse(history.getId(), history.getProductId(), history.getPrice(), history.getValidFrom());
  }
}
