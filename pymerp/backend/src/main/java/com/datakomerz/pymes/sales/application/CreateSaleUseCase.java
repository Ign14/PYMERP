package com.datakomerz.pymes.sales.application;

import com.datakomerz.pymes.sales.SalesService;
import com.datakomerz.pymes.sales.dto.SaleReq;
import com.datakomerz.pymes.sales.dto.SaleRes;
import org.springframework.stereotype.Component;

@Component
public class CreateSaleUseCase {

  private final SalesService service;

  public CreateSaleUseCase(SalesService service) {
    this.service = service;
  }

  public SaleRes handle(SaleReq request) {
    return service.create(request);
  }
}
