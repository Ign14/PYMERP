package com.datakomerz.pymes.customers;

import com.datakomerz.pymes.customers.dto.CustomerRequest;
import com.datakomerz.pymes.customers.dto.CustomerResponse;
import com.datakomerz.pymes.customers.dto.CustomerSegmentSummary;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

  private final CustomerService service;

  public CustomerController(CustomerService service) {
    this.service = service;
  }

  @GetMapping
  public Page<CustomerResponse> list(@RequestParam(name = "q", defaultValue = "") String q,
                                     @RequestParam(name = "segment", required = false) String segment,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
    return service.find(q, segment, pageable).map(this::toResponse);
  }

  @GetMapping("/segments")
  public List<CustomerSegmentSummary> segments() {
    return service.summarizeSegments();
  }

  @GetMapping("/{id}")
  public CustomerResponse get(@PathVariable UUID id) {
    return toResponse(service.get(id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CustomerResponse create(@Valid @RequestBody CustomerRequest request) {
    return toResponse(service.create(request));
  }

  @PutMapping("/{id}")
  public CustomerResponse update(@PathVariable UUID id, @Valid @RequestBody CustomerRequest request) {
    return toResponse(service.update(id, request));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  private CustomerResponse toResponse(Customer customer) {
    return new CustomerResponse(
      customer.getId(),
      customer.getName(),
      customer.getAddress(),
      customer.getLat(),
      customer.getLng(),
      customer.getPhone(),
      customer.getEmail(),
      customer.getSegment()
    );
  }
}
