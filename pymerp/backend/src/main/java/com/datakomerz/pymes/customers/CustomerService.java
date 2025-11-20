package com.datakomerz.pymes.customers;

import com.datakomerz.pymes.common.FieldValidationException;
import com.datakomerz.pymes.common.ValueNormalizer;
import com.datakomerz.pymes.core.tenancy.CompanyContext;
import com.datakomerz.pymes.customers.dto.CustomerRequest;
import com.datakomerz.pymes.customers.dto.CustomerSaleHistoryItem;
import com.datakomerz.pymes.customers.dto.CustomerSegmentSummary;
import com.datakomerz.pymes.customers.dto.CustomerStatsResponse;
import com.datakomerz.pymes.multitenancy.ValidateTenant;
import com.datakomerz.pymes.sales.Sale;
import com.datakomerz.pymes.sales.SaleRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class CustomerService {

  private final CustomerRepository repository;
  private final CompanyContext companyContext;
  private final SaleRepository saleRepository;
  private final ValueNormalizer valueNormalizer;

  public CustomerService(CustomerRepository repository,
                         CompanyContext companyContext,
                         SaleRepository saleRepository,
                         ValueNormalizer valueNormalizer) {
    this.repository = repository;
    this.companyContext = companyContext;
    this.saleRepository = saleRepository;
    this.valueNormalizer = valueNormalizer;
  }

  public static final String UNASSIGNED_SEGMENT_CODE = "__UNASSIGNED__";

  @Transactional(Transactional.TxType.SUPPORTS)
  public Page<Customer> find(String query, String segment, Pageable pageable) {
    return search(query, segment, null, pageable);
  }

  @Cacheable(value = "customers", key = "T(com.datakomerz.pymes.multitenancy.TenantContext).require() + ':all:' + #pageable.pageNumber")
  @Transactional(Transactional.TxType.SUPPORTS)
  public Page<Customer> findAll(Pageable pageable) {
    companyContext.require();
    return repository.searchCustomers(null, null, Boolean.TRUE, pageable);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Transactional(Transactional.TxType.SUPPORTS)
  public Page<Customer> findAllIncludingInactive(Pageable pageable) {
    return searchIncludingInactive(null, null, pageable);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Transactional(Transactional.TxType.SUPPORTS)
  public Page<Customer> searchIncludingInactive(String query, String segment, Pageable pageable) {
    UUID companyId = companyContext.require();
    String normalizedSearch = normalizeSearch(query);
    String normalizedSegment = normalizeSegment(segment);
    return repository.searchCustomersIncludingInactive(
      normalizedSearch,
      normalizedSegment,
      companyId,
      pageable
    );
  }

  @Transactional(Transactional.TxType.SUPPORTS)
  public Page<Customer> search(String query, String segment, Boolean active, Pageable pageable) {
    companyContext.require();
    String normalizedSearch = normalizeSearch(query);
    String normalizedSegment = normalizeSegment(segment);
    return repository.searchCustomers(
      normalizedSearch,
      normalizedSegment,
      active,
      pageable
    );
  }

  @CacheEvict(value = "customers", allEntries = true)
  public Customer create(CustomerRequest request) {
    UUID companyId = companyContext.require();
    String normalizedEmail = valueNormalizer.normalizeEmail(request.email());
    ensureUniqueEmail(companyId, normalizedEmail, null);

    Customer entity = new Customer();
    entity.setCompanyId(companyId);
    apply(entity, request, normalizedEmail);
    return repository.save(entity);
  }

  @ValidateTenant(entityClass = Customer.class, entityParamIndex = 0)
  @CacheEvict(value = "customers", allEntries = true)
  public Customer update(UUID id, CustomerRequest request) {
    companyContext.require();
    Customer entity = repository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
    String normalizedEmail = valueNormalizer.normalizeEmail(request.email());
    ensureUniqueEmail(entity.getCompanyId(), normalizedEmail, entity.getId());
    apply(entity, request, normalizedEmail);
    return repository.save(entity);
  }

  @ValidateTenant(entityClass = Customer.class, entityParamIndex = 0)
  @Caching(evict = {
    @CacheEvict(value = "customers", key = "T(com.datakomerz.pymes.multitenancy.TenantContext).require() + ':' + #id"),
    @CacheEvict(value = "customers", allEntries = true)
  })
  public void delete(UUID id) {
    companyContext.require();
    Customer entity = repository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
    repository.delete(entity);
  }

  @ValidateTenant(entityClass = Customer.class, entityParamIndex = 0)
  @Cacheable(value = "customers", key = "T(com.datakomerz.pymes.multitenancy.TenantContext).require() + ':' + #id")
  @Transactional(Transactional.TxType.SUPPORTS)
  public Customer get(UUID id) {
    companyContext.require();
    return repository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
  }

  @Transactional(Transactional.TxType.SUPPORTS)
  public List<CustomerSegmentSummary> summarizeSegments() {
    companyContext.require();
    return repository.summarizeBySegment().stream()
      .map(view -> new CustomerSegmentSummary(
        view.getSegment() == null || view.getSegment().isBlank() ? "Sin segmentar" : view.getSegment(),
        view.getSegment() == null || view.getSegment().isBlank() ? UNASSIGNED_SEGMENT_CODE : view.getSegment(),
        view.getTotal()
      ))
      .toList();
  }

  private void apply(Customer entity, CustomerRequest request, String normalizedEmail) {
    entity.setName(valueNormalizer.normalize(request.name()));
    entity.setRut(normalizeRut(request.rut()));
    entity.setAddress(valueNormalizer.normalize(request.address()));
    entity.setLat(request.lat());
    entity.setLng(request.lng());
    entity.setPhone(valueNormalizer.normalize(request.phone()));
    entity.setEmail(normalizedEmail);
    entity.setSegment(valueNormalizer.normalize(request.segment()));
    entity.setContactPerson(valueNormalizer.normalize(request.contactPerson()));
    entity.setNotes(valueNormalizer.normalize(request.notes()));
    if (request.active() != null) {
      entity.setActive(request.active());
    }
  }

  private String normalizeSearch(String query) {
    return valueNormalizer.normalizeSearch(query);
  }

  private String normalizeSegment(String segment) {
    return valueNormalizer.normalizeSegment(segment, UNASSIGNED_SEGMENT_CODE);
  }

  @Transactional(Transactional.TxType.SUPPORTS)
  @ValidateTenant(entityClass = Customer.class, entityParamIndex = 0)
  public CustomerStatsResponse getCustomerStats(UUID customerId) {
    repository.findById(customerId)
      .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + customerId));

    Integer totalSales = saleRepository.countByCustomerId(customerId);
    BigDecimal totalRevenue = saleRepository.sumTotalByCustomerId(customerId);
    
    // Get last sale date
    Page<Sale> recentSales = saleRepository.findByCustomerIdOrderByIssuedAtDesc(
      customerId, 
      PageRequest.of(0, 1)
    );
    LocalDate lastSaleDate = recentSales.hasContent() 
      ? recentSales.getContent().get(0).getIssuedAt().toLocalDate()
      : null;

    // For now, return empty top products list (can be enhanced later with SaleItem queries)
    return new CustomerStatsResponse(
      totalSales,
      totalRevenue != null ? totalRevenue : BigDecimal.ZERO,
      lastSaleDate,
      List.of()
    );
  }

  @Transactional(Transactional.TxType.SUPPORTS)
  @ValidateTenant(entityClass = Customer.class, entityParamIndex = 0)
  public Page<CustomerSaleHistoryItem> getCustomerSaleHistory(UUID customerId, Pageable pageable) {
    repository.findById(customerId)
      .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + customerId));

    Page<Sale> sales = saleRepository.findByCustomerIdOrderByIssuedAtDesc(
      customerId, 
      pageable
    );

    return sales.map(sale -> new CustomerSaleHistoryItem(
      sale.getId().toString(),
      sale.getIssuedAt().toInstant(),
      sale.getDocType(),
      sale.getId().toString(), // Using ID as docNumber since Sale doesn't have docNumber field
      sale.getTotal(),
      0 // Items count - would need SaleItem relationship
    ));
  }

  /**
   * Export customers to CSV format with applied filters.
   */
  @Transactional(Transactional.TxType.SUPPORTS)
  public List<Customer> exportToCSV(String query, String segment, Boolean active) {
    companyContext.require();
    String normalizedSearch = normalizeSearch(query);
    String normalizedSegment = normalizeSegment(segment);

    // Get all customers without pagination
    return repository.searchCustomers(
      normalizedSearch,
      normalizedSegment,
      active,
      Pageable.unpaged()
    ).getContent();
  }

  private void ensureUniqueEmail(UUID companyId, String normalizedEmail, UUID excludeId) {
    if (normalizedEmail == null || companyId == null) {
      return;
    }
    boolean taken = repository.existsByCompanyIdAndEmailIgnoreCase(companyId, normalizedEmail, excludeId);
    if (taken) {
      throw new FieldValidationException("email", "Ya existe un cliente con este email");
    }
  }

  private String normalizeRut(String value) {
    String normalized = valueNormalizer.normalize(value);
    if (normalized == null) {
      return null;
    }
    String cleaned = normalized.replace(".", "").replace("-", "").toUpperCase();
    if (cleaned.length() <= 1) {
      return cleaned;
    }
    String body = cleaned.substring(0, cleaned.length() - 1);
    String dv = cleaned.substring(cleaned.length() - 1);
    return body + "-" + dv;
  }
}
