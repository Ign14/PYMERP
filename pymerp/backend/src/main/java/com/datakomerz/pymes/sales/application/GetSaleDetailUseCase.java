package com.datakomerz.pymes.sales.application;

import com.datakomerz.pymes.sales.SalesService;
import com.datakomerz.pymes.sales.dto.SaleDetail;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GetSaleDetailUseCase {

  private final SalesService service;

  public GetSaleDetailUseCase(SalesService service) {
    this.service = service;
  }

  public SaleDetail handle(UUID id) {
    return service.detail(id);
  }
}
