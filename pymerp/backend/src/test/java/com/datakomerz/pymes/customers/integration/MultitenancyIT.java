package com.datakomerz.pymes.customers.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.datakomerz.pymes.auth.UserAccount;
import com.datakomerz.pymes.auth.UserAccountRepository;
import com.datakomerz.pymes.config.TestJwtDecoderConfig;
import com.datakomerz.pymes.customers.Customer;
import com.datakomerz.pymes.customers.CustomerService;
import com.datakomerz.pymes.customers.dto.CustomerRequest;
import com.datakomerz.pymes.integration.AbstractIT;
import com.datakomerz.pymes.multitenancy.TenantContext;
import com.datakomerz.pymes.multitenancy.TenantInterceptor;
import com.datakomerz.pymes.security.AppUserDetails;
import com.datakomerz.pymes.security.jwt.JwtService;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import({TestJwtDecoderConfig.class, CustomersIT.CacheTestConfig.class})
class MultitenancyIT extends AbstractIT {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtService jwtService;

  @Autowired
  private UserAccountRepository userAccountRepository;

  @Autowired
  private CustomerService customerService;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private UUID tenantA;
  private UUID tenantB;
  private String adminToken;

  @BeforeEach
  void setUp() {
    jdbcTemplate.execute("TRUNCATE TABLE customers, users CASCADE;");
    tenantA = UUID.randomUUID();
    tenantB = UUID.randomUUID();
    adminToken = buildTokenForTenant(tenantA);
  }

  @Test
  void blocksCrossTenantReadsAndDeletes() throws Exception {
    Customer customer = createCustomer(tenantA, "Tenant A Cliente");

    mockMvc.perform(get("/api/v1/customers/{id}", customer.getId())
        .header(HttpHeaders.AUTHORIZATION, adminToken)
        .header(TenantInterceptor.TENANT_HEADER, tenantB.toString()))
      .andExpect(status().isForbidden());

    mockMvc.perform(delete("/api/v1/customers/{id}", customer.getId())
        .header(HttpHeaders.AUTHORIZATION, adminToken)
        .header(TenantInterceptor.TENANT_HEADER, tenantB.toString()))
      .andExpect(status().isForbidden());
  }

  private Customer createCustomer(UUID tenantId, String name) {
    return runAsTenant(tenantId, () -> customerService.create(customerRequest(name)));
  }

  private CustomerRequest customerRequest(String name) {
    return new CustomerRequest(
      name,
      "12.345.678-9",
      "Calle de prueba",
      BigDecimal.ZERO,
      BigDecimal.ZERO,
      "+56900000000",
      "cliente@demo.com",
      null,
      null,
      null,
      Boolean.TRUE
    );
  }

  private String buildTokenForTenant(UUID tenant) {
    UserAccount account = new UserAccount();
    account.setId(UUID.randomUUID());
    account.setCompanyId(tenant);
    account.setEmail("admin-" + tenant + "@example.com");
    account.setName("Integration Admin");
    account.setRole("ADMIN");
    account.setStatus("active");
    account.setPasswordHash("secret");
    account.setRoles("ROLE_ADMIN");
    UserAccount saved = userAccountRepository.save(account);
    return "Bearer " + jwtService.generateToken(new AppUserDetails(saved));
  }

  private <T> T runAsTenant(UUID tenantId, Supplier<T> action) {
    try {
      TenantContext.setTenantId(tenantId);
      return action.get();
    } finally {
      TenantContext.clear();
    }
  }
}
