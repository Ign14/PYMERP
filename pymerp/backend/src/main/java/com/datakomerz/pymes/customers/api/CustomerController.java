package com.datakomerz.pymes.customers.api;

import com.datakomerz.pymes.common.api.PagedResponse;
import com.datakomerz.pymes.customers.Customer;
import com.datakomerz.pymes.customers.CustomerService;
import com.datakomerz.pymes.customers.application.CreateCustomerUseCase;
import com.datakomerz.pymes.customers.application.CustomerMapper;
import com.datakomerz.pymes.customers.application.ListCustomersUseCase;
import com.datakomerz.pymes.customers.dto.CustomerRequest;
import com.datakomerz.pymes.customers.dto.CustomerResponse;
import com.datakomerz.pymes.customers.dto.CustomerSegmentSummary;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

  private final ListCustomersUseCase listCustomersUseCase;
  private final CreateCustomerUseCase createCustomerUseCase;
  private final CustomerService service;
  private final CustomerMapper mapper;

  public CustomerController(ListCustomersUseCase listCustomersUseCase,
                            CreateCustomerUseCase createCustomerUseCase,
                            CustomerService service,
                            CustomerMapper mapper) {
    this.listCustomersUseCase = listCustomersUseCase;
    this.createCustomerUseCase = createCustomerUseCase;
    this.service = service;
    this.mapper = mapper;
  }

  @GetMapping
  public PagedResponse<CustomerResponse> list(@RequestParam(name = "q", defaultValue = "") String q,
                                              @RequestParam(name = "segment", required = false) String segment,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
    return listCustomersUseCase.handle(q, segment, page, size);
  }

  @GetMapping("/segments")
  public List<CustomerSegmentSummary> segments() {
    return service.summarizeSegments();
  }

  @GetMapping("/{id}")
  public CustomerResponse get(@PathVariable UUID id) {
    return mapper.toResponse(service.get(id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CustomerResponse create(@Valid @RequestBody CustomerRequest request) {
    return createCustomerUseCase.handle(request);
  }

  @PutMapping("/{id}")
  public CustomerResponse update(@PathVariable UUID id, @Valid @RequestBody CustomerRequest request) {
    Customer updated = service.update(id, request);
    return mapper.toResponse(updated);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
