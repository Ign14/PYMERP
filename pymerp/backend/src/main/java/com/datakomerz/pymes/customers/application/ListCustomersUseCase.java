package com.datakomerz.pymes.customers.application;

import com.datakomerz.pymes.common.api.PagedResponse;
import com.datakomerz.pymes.customers.CustomerService;
import com.datakomerz.pymes.customers.dto.CustomerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ListCustomersUseCase {

  private final CustomerService service;
  private final CustomerMapper mapper;

  public ListCustomersUseCase(CustomerService service, CustomerMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  public PagedResponse<CustomerResponse> handle(String query, String segment, Boolean active, int page, int size) {
    int pageIndex = Math.max(page, 0);
    int pageSize = size <= 0 ? 20 : Math.min(size, 100);
    Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by("createdAt").descending());
    boolean hasQuery = StringUtils.hasText(query);
    boolean hasSegment = StringUtils.hasText(segment);
    boolean activeOnly = active == null || Boolean.TRUE.equals(active);
    Page<CustomerResponse> result;
    if (!hasQuery && !hasSegment && activeOnly) {
      result = service.findAll(pageable).map(mapper::toResponse);
    } else {
      result = service.search(query, segment, active, pageable).map(mapper::toResponse);
    }
    return PagedResponse.from(result);
  }
}
