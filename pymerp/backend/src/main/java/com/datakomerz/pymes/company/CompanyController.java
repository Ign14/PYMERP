package com.datakomerz.pymes.company;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.datakomerz.pymes.company.dto.CompanyCreateRequest;
import com.datakomerz.pymes.company.dto.CompanyResponse;
import com.datakomerz.pymes.company.dto.ParentLocationResponse;

@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

  private final CompanyService service;

  public CompanyController(CompanyService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public List<CompanyResponse> list() {
    return service.findAllWithDetails();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  public CompanyResponse create(@Valid @RequestBody CompanyCreateRequest request) {
    return service.createWithDetails(request);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public CompanyResponse get(@PathVariable UUID id) {
    return service.getWithDetails(id);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
  public CompanyResponse update(@PathVariable UUID id, @Valid @RequestBody CompanyCreateRequest request) {
    return service.updateWithDetails(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }

  @GetMapping("/{id}/parent-locations")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public List<ParentLocationResponse> getParentLocations(@PathVariable UUID id) {
    return service.getParentLocations(id);
  }

  private CompanyResponse toResponse(Company company) {
    return new CompanyResponse(
      company.getId(),
      company.getBusinessName(),
      company.getFantasyName(),
      company.getRut(),
      company.getLogoUrl(),
      company.getBusinessActivity(),
      company.getAddress(),
      company.getCommune(),
      company.getPhone(),
      company.getEmail(),
      company.getReceiptFooterMessage(),
      null, // parentLocations - se obtienen por endpoint separado
      company.getCreatedAt(),
      company.getUpdatedAt()
    );
  }
}
