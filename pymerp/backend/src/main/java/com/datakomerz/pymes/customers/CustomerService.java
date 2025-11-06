package com.datakomerz.pymes.customers;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class CustomerService {

  private final CustomerRepository repository;
  private final CompanyContext companyContext;
  private final SaleRepository saleRepository;

  public CustomerService(CustomerRepository repository, CompanyContext companyContext, SaleRepository saleRepository) {
    this.repository = repository;
    this.companyContext = companyContext;
    this.saleRepository = saleRepository;
  }

  public static final String UNASSIGNED_SEGMENT_CODE = "__UNASSIGNED__";

  @Transactional(Transactional.TxType.SUPPORTS)
  public Page<Customer> find(String query, String segment, Pageable pageable) {
    return search(query, segment, null, pageable);
  }

  @Transactional(Transactional.TxType.SUPPORTS)
  public Page<Customer> search(String query, String segment, Boolean active, Pageable pageable) {
    companyContext.require();
    String search = query == null ? "" : query.trim();
    String normalizedSegment = segment == null ? null : segment.trim();
    
    // Handle special unassigned segment code
    if (UNASSIGNED_SEGMENT_CODE.equalsIgnoreCase(normalizedSegment)) {
      normalizedSegment = null; // Will match segment IS NULL in query
    }
    
    return repository.searchCustomers(
      search.isEmpty() ? null : search,
      normalizedSegment,
      active,
      pageable
    );
  }

  public Customer create(CustomerRequest request) {
    Customer entity = new Customer();
    entity.setCompanyId(companyContext.require());
    apply(entity, request);
    return repository.save(entity);
  }

  @ValidateTenant(entityClass = Customer.class, entityParamIndex = 0)
  public Customer update(UUID id, CustomerRequest request) {
    companyContext.require();
    Customer entity = repository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
    apply(entity, request);
    return repository.save(entity);
  }

  @ValidateTenant(entityClass = Customer.class, entityParamIndex = 0)
  public void delete(UUID id) {
    companyContext.require();
    Customer entity = repository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
    // Soft delete: marcar como inactivo
    entity.setActive(false);
    repository.save(entity);
  }

  @ValidateTenant(entityClass = Customer.class, entityParamIndex = 0)
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

  private void apply(Customer entity, CustomerRequest request) {
    entity.setName(request.name().trim());
    entity.setRut(normalize(request.rut()));
    entity.setAddress(normalize(request.address()));
    entity.setLat(request.lat());
    entity.setLng(request.lng());
    entity.setPhone(normalize(request.phone()));
    entity.setEmail(normalize(request.email()));
    entity.setSegment(normalize(request.segment()));
    entity.setContactPerson(normalize(request.contactPerson()));
    entity.setNotes(normalize(request.notes()));
    if (request.active() != null) {
      entity.setActive(request.active());
    }
  }

  private String normalize(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
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
    String search = query == null ? "" : query.trim();
    String normalizedSegment = segment == null ? null : segment.trim();
    
    // Handle special unassigned segment code
    if (UNASSIGNED_SEGMENT_CODE.equalsIgnoreCase(normalizedSegment)) {
      normalizedSegment = null;
    }
    
    // Get all customers without pagination
    return repository.searchCustomers(
      search.isEmpty() ? null : search,
      normalizedSegment,
      active,
      Pageable.unpaged()
    ).getContent();
  }
}
