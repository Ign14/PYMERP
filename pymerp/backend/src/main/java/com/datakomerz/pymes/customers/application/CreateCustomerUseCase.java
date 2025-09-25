package com.datakomerz.pymes.customers.application;

import com.datakomerz.pymes.customers.CustomerService;
import com.datakomerz.pymes.customers.dto.CustomerRequest;
import com.datakomerz.pymes.customers.dto.CustomerResponse;
import org.springframework.stereotype.Component;

@Component
public class CreateCustomerUseCase {

  private final CustomerService service;
  private final CustomerMapper mapper;

  public CreateCustomerUseCase(CustomerService service, CustomerMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  public CustomerResponse handle(CustomerRequest request) {
    return mapper.toResponse(service.create(request));
  }
}
