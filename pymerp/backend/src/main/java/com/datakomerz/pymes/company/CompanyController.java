package com.datakomerz.pymes.company;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.datakomerz.pymes.company.dto.CompanyRequest;
import com.datakomerz.pymes.company.dto.CompanyResponse;

@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

  private final CompanyService service;

  public CompanyController(CompanyService service) {
    this.service = service;
  }

  @GetMapping
  public List<CompanyResponse> list() {
    return service.findAll().stream().map(this::toResponse).toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CompanyResponse create(@Valid @RequestBody CompanyRequest request) {
    return toResponse(service.create(request));
  }

  @GetMapping("/{id}")
  public CompanyResponse get(@PathVariable UUID id) {
    return toResponse(service.get(id));
  }

  @PutMapping("/{id}")
  public CompanyResponse update(@PathVariable UUID id, @Valid @RequestBody CompanyRequest request) {
    return toResponse(service.update(id, request));
  }

  private CompanyResponse toResponse(Company company) {
    return new CompanyResponse(
      company.getId(),
      company.getBusinessName(),
      company.getRut(),
      company.getBusinessActivity(),
      company.getAddress(),
      company.getCommune(),
      company.getPhone(),
      company.getEmail(),
      company.getReceiptFooterMessage(),
      company.getCreatedAt(),
      company.getUpdatedAt()
    );
  }
}
