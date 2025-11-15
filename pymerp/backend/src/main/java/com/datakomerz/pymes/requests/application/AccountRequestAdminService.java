package com.datakomerz.pymes.requests.application;

import com.datakomerz.pymes.auth.UserAccount;
import com.datakomerz.pymes.auth.UserAccountRepository;
import com.datakomerz.pymes.company.Company;
import com.datakomerz.pymes.company.CompanyService;
import com.datakomerz.pymes.company.dto.CompanyRequest;
import com.datakomerz.pymes.requests.AccountRequest;
import com.datakomerz.pymes.requests.AccountRequestRepository;
import com.datakomerz.pymes.requests.AccountRequestStatus;
import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class AccountRequestAdminService {

  private static final Logger LOG = LoggerFactory.getLogger(AccountRequestAdminService.class);

  private final AccountRequestRepository repository;
  private final CompanyService companyService;
  private final UserAccountRepository userAccountRepository;

  public AccountRequestAdminService(AccountRequestRepository repository,
                                    CompanyService companyService,
                                    UserAccountRepository userAccountRepository) {
    this.repository = repository;
    this.companyService = companyService;
    this.userAccountRepository = userAccountRepository;
  }

  @Transactional(readOnly = true)
  public List<AccountRequest> getPendingRequests() {
    return repository.findByStatusOrderByCreatedAtDesc(AccountRequestStatus.PENDING);
  }

  @Transactional(readOnly = true)
  public List<AccountRequest> getRecentRequests(int days) {
    int safeDays = Math.max(1, Math.min(days, 90));
    OffsetDateTime since = OffsetDateTime.now().minusDays(safeDays);
    return repository.findRecentRequests(since);
  }

  public void approve(UUID requestId, UserAccount admin) {
    AccountRequest request = repository.findById(requestId)
      .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));
    ensurePending(request);

    ensureEmailAvailable(request.getEmail());

    Company company = companyService.create(new CompanyRequest(
      request.getCompanyName(),
      request.getRut(),
      null,
      request.getAddress(),
      null,
      null,
      request.getEmail(),
      null
    ));

    UserAccount user = new UserAccount();
    user.setCompanyId(company.getId());
    user.setEmail(request.getEmail());
    user.setName(request.getFullName());
    user.setRole("admin");
    user.setStatus("active");
    user.setRoles("ROLE_ADMIN,ROLE_SETTINGS");
    user.setPasswordHash(request.getPasswordHash());
    userAccountRepository.save(user);

    request.setStatus(AccountRequestStatus.APPROVED);
    request.setProcessedAt(OffsetDateTime.now());
    request.setProcessedBy(admin.getId());
    request.setProcessedByUsername(admin.getEmail());
    request.setRejectionReason(null);
    repository.save(request);

    LOG.info("Account request {} approved by {}", requestId, admin.getEmail());
  }

  public void reject(UUID requestId, UserAccount admin, String reason) {
    AccountRequest request = repository.findById(requestId)
      .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));
    ensurePending(request);

    request.setStatus(AccountRequestStatus.REJECTED);
    request.setProcessedAt(OffsetDateTime.now());
    request.setProcessedBy(admin.getId());
    request.setProcessedByUsername(admin.getEmail());
    request.setRejectionReason(normalizeReason(reason));
    repository.save(request);

    LOG.info("Account request {} rejected by {}: {}", requestId, admin.getEmail(), reason);
  }

  private void ensurePending(AccountRequest request) {
    if (request.getStatus() != AccountRequestStatus.PENDING) {
      throw new IllegalStateException("La solicitud ya fue procesada");
    }
  }

  private void ensureEmailAvailable(String email) {
    if (!StringUtils.hasText(email)) {
      return;
    }
    userAccountRepository.findByEmailIgnoreCase(email.trim())
      .ifPresent(existing -> {
        throw new IllegalStateException("Ya existe un usuario registrado con este correo");
      });
  }

  private String normalizeReason(String reason) {
    if (!StringUtils.hasText(reason)) {
      return null;
    }
    String trimmed = reason.trim();
    if (trimmed.length() > 500) {
      return trimmed.substring(0, 500);
    }
    return trimmed;
  }
}
