package com.datakomerz.pymes.sales.application;

import com.datakomerz.pymes.sales.SalesService;
import com.datakomerz.pymes.sales.dto.SaleRes;
import com.datakomerz.pymes.sales.dto.SaleUpdateRequest;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UpdateSaleUseCase {

  private final SalesService service;

  public UpdateSaleUseCase(SalesService service) {
    this.service = service;
  }

  public SaleRes handle(UUID id, SaleUpdateRequest request) {
    return service.update(id, request);
  }
}
