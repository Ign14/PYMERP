package com.datakomerz.pymes.suppliers;

import com.datakomerz.pymes.core.tenancy.CompanyContext;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/suppliers")
public class SupplierController {
  private final SupplierRepository repo;
  private final SupplierContactRepository contacts;
  private final CompanyContext companyContext;

  public SupplierController(SupplierRepository repo, SupplierContactRepository contacts, CompanyContext companyContext) {
    this.repo = repo;
    this.contacts = contacts;
    this.companyContext = companyContext;
  }

  @GetMapping
  public List<Supplier> list() {
    return repo.findByCompanyIdOrderByNameAsc(companyContext.require());
  }

  @PostMapping
  public Supplier create(@Valid @RequestBody SupplierRequest request) {
    Supplier supplier = new Supplier();
    supplier.setCompanyId(companyContext.require());
    apply(supplier, request);
    return repo.save(supplier);
  }

  @PutMapping("/{id}")
  public Supplier update(@PathVariable UUID id, @Valid @RequestBody SupplierRequest request) {
    Supplier supplier = ensureOwnership(id);
    apply(supplier, request);
    return repo.save(supplier);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    Supplier supplier = ensureOwnership(id);
    repo.delete(supplier);
  }

  @GetMapping("/{id}/contacts")
  public List<SupplierContact> listContacts(@PathVariable UUID id) {
    ensureOwnership(id);
    return contacts.findBySupplierId(id);
  }

  @PostMapping("/{id}/contacts")
  public SupplierContact addContact(@PathVariable UUID id, @Valid @RequestBody SupplierContact contact) {
    ensureOwnership(id);
    contact.setSupplierId(id);
    return contacts.save(contact);
  }

  private void apply(Supplier supplier, SupplierRequest request) {
    supplier.setName(request.name().trim());
    supplier.setRut(normalize(request.rut()));
    supplier.setAddress(normalize(request.address()));
    supplier.setCommune(normalize(request.commune()));
    supplier.setBusinessActivity(normalize(request.businessActivity()));
    supplier.setPhone(normalize(request.phone()));
    supplier.setEmail(normalize(request.email()));
  }

  private Supplier ensureOwnership(UUID supplierId) {
    UUID companyId = companyContext.require();
    return repo.findByIdAndCompanyId(supplierId, companyId)
      .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + supplierId));
  }

  private String normalize(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
