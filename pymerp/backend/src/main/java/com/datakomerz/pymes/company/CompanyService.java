package com.datakomerz.pymes.company;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
    return repository.findAllByOrderByNameAsc();
  }

  public Company create(CompanyRequest request) {
    Company company = new Company();
    apply(company, request);
    return repository.save(company);
  }

  public Company update(UUID id, CompanyRequest request) {
    Company company = repository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Company not found: " + id));
    apply(company, request);
    return repository.save(company);
  }

  @Transactional(Transactional.TxType.SUPPORTS)
  public Company get(UUID id) {
    return repository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Company not found: " + id));
  }

  private void apply(Company company, CompanyRequest request) {
    company.setName(request.name().trim());
    company.setRut(normalize(request.rut()));
    company.setIndustry(normalize(request.industry()));
    company.setOpenTime(request.openTime());
    company.setCloseTime(request.closeTime());
    company.setReceiptFooter(normalize(request.receiptFooter()));
    company.setLogoUrl(normalize(request.logoUrl()));
  }

  private String normalize(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
