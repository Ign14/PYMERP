package com.datakomerz.pymes.company;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.datakomerz.pymes.common.validation.RutUtils;
import com.datakomerz.pymes.company.dto.CompanyRequest;

@Service
@Transactional
public class CompanyService {

  private final CompanyRepository repository;

  public CompanyService(CompanyRepository repository) {
    this.repository = repository;
  }

  @Transactional(Transactional.TxType.SUPPORTS)
  public List<Company> findAll() {
    return repository.findAllByOrderByBusinessNameAsc();
  }

  @CacheEvict(value = "companySettings", allEntries = true)
  public Company create(CompanyRequest request) {
    Company company = new Company();
    apply(company, request, null);
    return repository.save(company);
  }

  @CacheEvict(value = "companySettings", allEntries = true)
  public Company update(UUID id, CompanyRequest request) {
    Company company = repository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Company not found: " + id));
    apply(company, request, id);
    return repository.save(company);
  }

  @Cacheable(value = "companySettings", key = "#id")
  @Transactional(Transactional.TxType.SUPPORTS)
  public Company get(UUID id) {
    return repository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Company not found: " + id));
  }

  private void apply(Company company, CompanyRequest request, UUID currentId) {
    company.setBusinessName(request.businessName().trim());

    String normalizedRut = RutUtils.normalize(request.rut());
    ensureRutUnique(normalizedRut, currentId);
    company.setRut(normalizedRut);

    company.setBusinessActivity(normalize(request.businessActivity()));
    company.setAddress(normalize(request.address()));
    company.setCommune(normalize(request.commune()));
    company.setPhone(normalize(request.phone()));

    String email = normalize(request.email());
    company.setEmail(email != null ? email.toLowerCase() : null);

    company.setReceiptFooterMessage(normalize(request.receiptFooterMessage()));
  }

  private void ensureRutUnique(String rut, UUID currentId) {
    boolean exists = currentId == null
      ? repository.existsByRut(rut)
      : repository.existsByRutAndIdNot(rut, currentId);
    if (exists) {
      throw new IllegalArgumentException("Ya existe una compañía con este RUT");
    }
  }

  private String normalize(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
