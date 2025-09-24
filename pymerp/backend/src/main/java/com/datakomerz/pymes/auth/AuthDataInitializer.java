package com.datakomerz.pymes.auth;

import com.datakomerz.pymes.config.AppProperties;
import com.datakomerz.pymes.company.CompanyRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AuthDataInitializer implements CommandLineRunner {

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
    userAccountRepository.findByEmailIgnoreCase(adminEmail)
      .orElseGet(() -> {
        UUID companyId = resolveDefaultCompany();
        if (companyId == null) {
          return null;
        }
        UserAccount admin = new UserAccount();
        admin.setCompanyId(companyId);
        admin.setEmail(adminEmail);
        admin.setName("Dev Admin");
        admin.setRole("admin");
        admin.setStatus("active");
        admin.setRoles("ROLE_ADMIN");
        admin.setPasswordHash(passwordEncoder.encode("Admin1234"));
        return userAccountRepository.save(admin);
      });
  }

  private UUID resolveDefaultCompany() {
    UUID configured = appProperties.getTenancy().getDefaultCompanyId();
    if (configured != null) {
      return configured;
    }
    return companyRepository.findAll().stream().findFirst().map(c -> c.getId()).orElse(null);
  }
}
