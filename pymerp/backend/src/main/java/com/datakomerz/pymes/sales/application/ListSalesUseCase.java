package com.datakomerz.pymes.sales.application;

import com.datakomerz.pymes.sales.SalesService;
import com.datakomerz.pymes.sales.dto.SaleSummary;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class ListSalesUseCase {

  private final SalesService service;

  public ListSalesUseCase(SalesService service) {
    this.service = service;
  }

  public Page<SaleSummary> handle(String status,
                                  String docType,
                                  String paymentMethod,
                                  String search,
                                  OffsetDateTime from,
                                  OffsetDateTime to,
                                  int page,
                                  int size) {
    int pageIndex = Math.max(page, 0);
    int pageSize = size <= 0 ? 20 : Math.min(size, 100);
    return service.list(status, docType, paymentMethod, search, from, to, PageRequest.of(pageIndex, pageSize));
  }
}
