package com.datakomerz.pymes.sales.application;

import com.datakomerz.pymes.sales.SalesService;
import com.datakomerz.pymes.sales.dto.SaleRes;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CancelSaleUseCase {

  private final SalesService service;

  public CancelSaleUseCase(SalesService service) {
    this.service = service;
  }

  public SaleRes handle(UUID id) {
    return service.cancel(id);
  }
}
