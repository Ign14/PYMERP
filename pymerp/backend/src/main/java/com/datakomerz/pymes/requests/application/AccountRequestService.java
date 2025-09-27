package com.datakomerz.pymes.requests.application;

import com.datakomerz.pymes.common.captcha.SimpleCaptchaValidationService;
import com.datakomerz.pymes.common.validation.RutUtils;
import com.datakomerz.pymes.requests.AccountRequest;
import com.datakomerz.pymes.requests.AccountRequestRepository;
import com.datakomerz.pymes.requests.dto.AccountRequestPayload;
import com.datakomerz.pymes.requests.dto.AccountRequestResponse;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AccountRequestService {

  private static final Logger LOGGER = LoggerFactory.getLogger(AccountRequestService.class);
  private static final String SUCCESS_MESSAGE = "¡Muchas gracias! Te contactaremos lo antes posible. PYMERP.cl";

  private final AccountRequestRepository repository;
  private final PasswordEncoder passwordEncoder;
  private final AccountRequestNotifier notifier;
  private final SimpleCaptchaValidationService captchaValidationService;

  public AccountRequestService(AccountRequestRepository repository,
                               PasswordEncoder passwordEncoder,
                               AccountRequestNotifier notifier,
                               SimpleCaptchaValidationService captchaValidationService) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
    this.notifier = notifier;
    this.captchaValidationService = captchaValidationService;
  }

  @Transactional
  public AccountRequestResponse create(AccountRequestPayload payload) {
    captchaValidationService.validate(payload.captcha());
    if (!payload.passwordsMatch()) {
      throw new IllegalArgumentException("Las contraseñas no coinciden");
    }
    if (!RutUtils.isValid(payload.rut())) {
      throw new IllegalArgumentException("El RUT ingresado no es válido");
    }

    AccountRequest request = new AccountRequest();
    request.setRut(RutUtils.normalize(payload.rut()));
    request.setFullName(payload.fullName().trim());
    request.setAddress(payload.address().trim());
    request.setEmail(payload.email().trim().toLowerCase());
    request.setCompanyName(payload.companyName().trim());
    request.setPasswordHash(passwordEncoder.encode(payload.password()));

    AccountRequest saved = repository.save(request);
    try {
      notifier.notifyNewRequest(saved);
    } catch (MailException ex) {
      LOGGER.warn("Failed to send notification email for account request {}: {}", saved.getId(), ex.getMessage());
    }
    return new AccountRequestResponse(saved.getId(), saved.getStatus(), saved.getCreatedAt(), SUCCESS_MESSAGE);
  }
}
