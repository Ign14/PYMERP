package com.datakomerz.pymes.customers;

import com.datakomerz.pymes.core.tenancy.CompanyContext;
import com.datakomerz.pymes.customers.dto.CustomerRequest;
import com.datakomerz.pymes.customers.dto.CustomerSegmentSummary;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class CustomerService {

  private final CustomerRepository repository;
  private final CompanyContext companyContext;

  public CustomerService(CustomerRepository repository, CompanyContext companyContext) {
    this.repository = repository;
    this.companyContext = companyContext;
  }

  public static final String UNASSIGNED_SEGMENT_CODE = "__UNASSIGNED__";

  @Transactional(Transactional.TxType.SUPPORTS)
  public Page<Customer> find(String query, String segment, Pageable pageable) {
    UUID companyId = companyContext.require();
    String search = query == null ? "" : query.trim();
    String normalizedSegment = segment == null ? null : segment.trim();
    if (StringUtils.hasText(normalizedSegment)) {
      if (UNASSIGNED_SEGMENT_CODE.equalsIgnoreCase(normalizedSegment)) {
        return repository.findByCompanyIdAndSegmentIsNullAndNameContainingIgnoreCase(companyId, search, pageable);
      }
      return repository.findByCompanyIdAndSegmentIgnoreCaseAndNameContainingIgnoreCase(
        companyId,
        normalizedSegment,
        search,
        pageable
      );
    }
    return repository.findByCompanyIdAndNameContainingIgnoreCase(companyId, search, pageable);
  }

  public Customer create(CustomerRequest request) {
    Customer entity = new Customer();
    entity.setCompanyId(companyContext.require());
    apply(entity, request);
    return repository.save(entity);
  }

  public Customer update(UUID id, CustomerRequest request) {
    Customer entity = repository.findByIdAndCompanyId(id, companyContext.require())
      .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
    apply(entity, request);
    return repository.save(entity);
  }

  public void delete(UUID id) {
    Customer entity = repository.findByIdAndCompanyId(id, companyContext.require())
      .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
    repository.delete(entity);
  }

  @Transactional(Transactional.TxType.SUPPORTS)
  public Customer get(UUID id) {
    return repository.findByIdAndCompanyId(id, companyContext.require())
      .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
  }

  @Transactional(Transactional.TxType.SUPPORTS)
  public List<CustomerSegmentSummary> summarizeSegments() {
    UUID companyId = companyContext.require();
    return repository.summarizeBySegment(companyId).stream()
      .map(view -> new CustomerSegmentSummary(
        view.getSegment() == null || view.getSegment().isBlank() ? "Sin segmentar" : view.getSegment(),
        view.getSegment() == null || view.getSegment().isBlank() ? UNASSIGNED_SEGMENT_CODE : view.getSegment(),
        view.getTotal()
      ))
      .toList();
  }

  private void apply(Customer entity, CustomerRequest request) {
    entity.setName(request.name().trim());
    entity.setAddress(normalize(request.address()));
    entity.setLat(request.lat());
    entity.setLng(request.lng());
    entity.setPhone(normalize(request.phone()));
    entity.setEmail(normalize(request.email()));
    entity.setSegment(normalize(request.segment()));
  }

  private String normalize(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
