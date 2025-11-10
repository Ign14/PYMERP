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
import com.datakomerz.pymes.company.dto.CompanyCreateRequest;
import com.datakomerz.pymes.company.dto.CompanyResponse;
import com.datakomerz.pymes.company.dto.ParentLocationRequest;
import com.datakomerz.pymes.company.dto.ParentLocationResponse;

@Service
@Transactional
public class CompanyService {

  private final CompanyRepository repository;
  private final CompanyParentLocationRepository parentLocationRepository;

  public CompanyService(CompanyRepository repository, 
                       CompanyParentLocationRepository parentLocationRepository) {
    this.repository = repository;
    this.parentLocationRepository = parentLocationRepository;
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

  @CacheEvict(value = "companySettings", allEntries = true)
  public CompanyResponse createWithDetails(CompanyCreateRequest request) {
    Company company = new Company();
    company.setBusinessName(request.businessName().trim());
    company.setFantasyName(normalize(request.fantasyName()));
    
    String normalizedRut = RutUtils.normalize(request.rut());
    ensureRutUnique(normalizedRut, null);
    company.setRut(normalizedRut);
    
    company.setLogoUrl(normalize(request.logoUrl()));
    company.setBusinessActivity(normalize(request.businessActivity()));
    company.setAddress(normalize(request.address()));
    company.setCommune(normalize(request.commune()));
    company.setPhone(normalize(request.phone()));
    
    String email = normalize(request.email());
    company.setEmail(email != null ? email.toLowerCase() : null);
    company.setReceiptFooterMessage(normalize(request.receiptFooterMessage()));
    
    company = repository.save(company);
    
    // Guardar ubicaciones padre
    List<ParentLocationResponse> parentLocations = new java.util.ArrayList<>();
    if (request.parentLocations() != null && !request.parentLocations().isEmpty()) {
      for (ParentLocationRequest locReq : request.parentLocations()) {
        CompanyParentLocation parentLoc = new CompanyParentLocation();
        parentLoc.setCompanyId(company.getId());
        parentLoc.setName(locReq.name().trim());
        parentLoc.setCode(locReq.code().trim());
        parentLoc = parentLocationRepository.save(parentLoc);
        parentLocations.add(new ParentLocationResponse(parentLoc.getId(), parentLoc.getName(), parentLoc.getCode()));
      }
    }
    
    return toResponse(company, parentLocations);
  }

  @CacheEvict(value = "companySettings", allEntries = true)
  public CompanyResponse updateWithDetails(UUID id, CompanyCreateRequest request) {
    Company company = repository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Company not found: " + id));
    
    company.setBusinessName(request.businessName().trim());
    company.setFantasyName(normalize(request.fantasyName()));
    
    String normalizedRut = RutUtils.normalize(request.rut());
    ensureRutUnique(normalizedRut, id);
    company.setRut(normalizedRut);
    
    company.setLogoUrl(normalize(request.logoUrl()));
    company.setBusinessActivity(normalize(request.businessActivity()));
    company.setAddress(normalize(request.address()));
    company.setCommune(normalize(request.commune()));
    company.setPhone(normalize(request.phone()));
    
    String email = normalize(request.email());
    company.setEmail(email != null ? email.toLowerCase() : null);
    company.setReceiptFooterMessage(normalize(request.receiptFooterMessage()));
    
    company = repository.save(company);
    
    // Actualizar ubicaciones padre (eliminar antiguas y crear nuevas)
    parentLocationRepository.deleteByCompanyId(id);
    
    List<ParentLocationResponse> parentLocations = new java.util.ArrayList<>();
    if (request.parentLocations() != null && !request.parentLocations().isEmpty()) {
      for (ParentLocationRequest locReq : request.parentLocations()) {
        CompanyParentLocation parentLoc = new CompanyParentLocation();
        parentLoc.setCompanyId(company.getId());
        parentLoc.setName(locReq.name().trim());
        parentLoc.setCode(locReq.code().trim());
        parentLoc = parentLocationRepository.save(parentLoc);
        parentLocations.add(new ParentLocationResponse(parentLoc.getId(), parentLoc.getName(), parentLoc.getCode()));
      }
    }
    
    return toResponse(company, parentLocations);
  }

  @Transactional(Transactional.TxType.SUPPORTS)
  public List<CompanyResponse> findAllWithDetails() {
    List<Company> companies = repository.findAllByOrderByBusinessNameAsc();
    return companies.stream()
      .map(c -> {
        List<CompanyParentLocation> parentLocs = parentLocationRepository.findByCompanyId(c.getId());
        List<ParentLocationResponse> parentLocResponses = parentLocs.stream()
          .map(pl -> new ParentLocationResponse(pl.getId(), pl.getName(), pl.getCode()))
          .toList();
        return toResponse(c, parentLocResponses);
      })
      .toList();
  }

  @Transactional(Transactional.TxType.SUPPORTS)
  public CompanyResponse getWithDetails(UUID id) {
    Company company = repository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Company not found: " + id));
    
    List<CompanyParentLocation> parentLocs = parentLocationRepository.findByCompanyId(id);
    List<ParentLocationResponse> parentLocResponses = parentLocs.stream()
      .map(pl -> new ParentLocationResponse(pl.getId(), pl.getName(), pl.getCode()))
      .toList();
    
    return toResponse(company, parentLocResponses);
  }

  @CacheEvict(value = "companySettings", allEntries = true)
  public void delete(UUID id) {
    if (!repository.existsById(id)) {
      throw new EntityNotFoundException("Company not found: " + id);
    }
    parentLocationRepository.deleteByCompanyId(id);
    repository.deleteById(id);
  }

  @Transactional(Transactional.TxType.SUPPORTS)
  public List<ParentLocationResponse> getParentLocations(UUID companyId) {
    List<CompanyParentLocation> parentLocs = parentLocationRepository.findByCompanyId(companyId);
    return parentLocs.stream()
      .map(pl -> new ParentLocationResponse(pl.getId(), pl.getName(), pl.getCode()))
      .toList();
  }

  private CompanyResponse toResponse(Company c, List<ParentLocationResponse> parentLocs) {
    return new CompanyResponse(
      c.getId(),
      c.getBusinessName(),
      c.getFantasyName(),
      c.getRut(),
      c.getLogoUrl(),
      c.getBusinessActivity(),
      c.getAddress(),
      c.getCommune(),
      c.getPhone(),
      c.getEmail(),
      c.getReceiptFooterMessage(),
      parentLocs,
      c.getCreatedAt(),
      c.getUpdatedAt()
    );
  }
}
