package com.datakomerz.pymes.auth;

import com.datakomerz.pymes.company.Company;
import com.datakomerz.pymes.common.validation.RutUtils;
import com.datakomerz.pymes.config.AppProperties;
import com.datakomerz.pymes.company.CompanyRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AuthDataInitializer implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(AuthDataInitializer.class);

  private final UserAccountRepository userAccountRepository;
  private final CompanyRepository companyRepository;
  private final PasswordEncoder passwordEncoder;
  private final AppProperties appProperties;

  public AuthDataInitializer(UserAccountRepository userAccountRepository,
                             CompanyRepository companyRepository,
                             PasswordEncoder passwordEncoder,
                             AppProperties appProperties) {
    this.userAccountRepository = userAccountRepository;
    this.companyRepository = companyRepository;
    this.passwordEncoder = passwordEncoder;
    this.appProperties = appProperties;
  }

  @Override
  public void run(String... args) {
    String adminEmail = "admin@dev.local";
    if (userAccountRepository.findByEmailIgnoreCase(adminEmail).isPresent()) {
      return;
    }

    UUID companyId = resolveDefaultCompany();
    if (companyId == null) {
      log.warn("Skipping default admin bootstrap because no company could be resolved");
      return;
    }

    companyRepository.findById(companyId).orElseGet(() -> {
      Company company = new Company();
      company.setId(companyId);
      company.setBusinessName("Dev Company");
      company.setRut(RutUtils.normalize("76.000.000-0"));
      return companyRepository.save(company);
    });

    UserAccount admin = new UserAccount();
    admin.setCompanyId(companyId);
    admin.setEmail(adminEmail);
    admin.setName("Dev Admin");
    admin.setRole("admin");
    admin.setStatus("active");
    admin.setRoles("ROLE_ADMIN");
    admin.setPasswordHash(passwordEncoder.encode("Admin1234"));
    userAccountRepository.save(admin);
  }

  private UUID resolveDefaultCompany() {
    UUID configured = appProperties.getTenancy().getDefaultCompanyId();
    if (configured != null) {
      return configured;
    }
    return companyRepository.findAll().stream().findFirst().map(c -> c.getId()).orElse(null);
  }
}
