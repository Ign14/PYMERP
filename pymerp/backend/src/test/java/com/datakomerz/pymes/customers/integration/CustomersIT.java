package com.datakomerz.pymes.customers.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import({TestJwtDecoderConfig.class, CustomersIT.CacheTestConfig.class})
class CustomersIT extends AbstractIT {

  @DynamicPropertySource
  static void disableRedisCache(DynamicPropertyRegistry registry) {
    registry.add("app.cache.redis.enabled", () -> "false");
  }

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private CustomerService customerService;

  @Autowired
  private UserAccountRepository userAccountRepository;

  @Autowired
  private JwtService jwtService;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private CacheManager cacheManager;

  private UUID tenantA;
  private UUID tenantB;
  private Customer tenantACustomer;
  private String adminToken;

  @BeforeEach
  void setUp() {
    jdbcTemplate.execute("TRUNCATE TABLE customers, users CASCADE;");
    tenantA = UUID.randomUUID();
    tenantB = UUID.randomUUID();
    UserAccount admin = createAdminAccount(tenantA);
    adminToken = jwtService.generateToken(new AppUserDetails(admin));
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
    Cache cache = cacheManager.getCache("customers");
    if (cache != null) {
      cache.clear();
    }
  }

  @Test
  void deleteSoftRemovesCustomerFromDefaultListAndEvictsCache() throws Exception {
    tenantACustomer = createCustomer(tenantA, "Acme Cliente");
    fetchCustomerPage(tenantA, false); // prime cache
    MvcResult before = fetchCustomerPage(tenantA, false);
    assertCustomerCount(before, 1);

    mockMvc.perform(delete("/api/v1/customers/{id}", tenantACustomer.getId())
        .header(HttpHeaders.AUTHORIZATION, token())
        .header(TenantInterceptor.TENANT_HEADER, tenantA.toString()))
      .andExpect(status().isNoContent());

    MvcResult after = fetchCustomerPage(tenantA, false);
    assertCustomerCount(after, 0);
  }

  @Test
  void includeInactiveFlagReturnsSoftDeletedEntity() throws Exception {
    tenantACustomer = createCustomer(tenantA, "Acme Cliente");
    mockMvc.perform(delete("/api/v1/customers/{id}", tenantACustomer.getId())
        .header(HttpHeaders.AUTHORIZATION, token())
        .header(TenantInterceptor.TENANT_HEADER, tenantA.toString()))
      .andExpect(status().isNoContent());

    MvcResult response = fetchCustomerPage(tenantA, true);
    assertCustomerCount(response, 1);
    JsonNode entry = objectMapper.readTree(response.getResponse().getContentAsString()).get("data").get(0);
    assertFalse(entry.get("active").asBoolean());
  }

  @Test
  void lifecycleCreateListUpdateDeleteRespectsSoftDeleteFilters() throws Exception {
    CustomerRequest initialRequest = customerRequest("Lifecycle Cliente");
    MvcResult creation = mockMvc.perform(post("/api/v1/customers")
        .header(HttpHeaders.AUTHORIZATION, token())
        .header(TenantInterceptor.TENANT_HEADER, tenantA.toString())
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(initialRequest)))
      .andExpect(status().isCreated())
      .andReturn();

    JsonNode createdBody = objectMapper.readTree(creation.getResponse().getContentAsString());
    UUID customerId = UUID.fromString(createdBody.get("id").asText());

    MvcResult beforeUpdate = fetchCustomerPage(tenantA, false);
    assertCustomerCount(beforeUpdate, 1);

    CustomerRequest updateRequest = customerRequest("Lifecycle Cliente Actualizado");
    mockMvc.perform(put("/api/v1/customers/{id}", customerId)
        .header(HttpHeaders.AUTHORIZATION, token())
        .header(TenantInterceptor.TENANT_HEADER, tenantA.toString())
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(updateRequest)))
      .andExpect(status().isOk());

    JsonNode updated = objectMapper.readTree(mockMvc.perform(get("/api/v1/customers/{id}", customerId)
          .header(HttpHeaders.AUTHORIZATION, token())
          .header(TenantInterceptor.TENANT_HEADER, tenantA.toString()))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString());
    assertEquals("Lifecycle Cliente Actualizado", updated.get("name").asText());

    mockMvc.perform(delete("/api/v1/customers/{id}", customerId)
        .header(HttpHeaders.AUTHORIZATION, token())
        .header(TenantInterceptor.TENANT_HEADER, tenantA.toString()))
      .andExpect(status().isNoContent());

    MvcResult afterDeletion = fetchCustomerPage(tenantA, false);
    assertCustomerCount(afterDeletion, 0);

    MvcResult includeInactive = fetchCustomerPage(tenantA, true);
    assertCustomerCount(includeInactive, 1);
    JsonNode entry = objectMapper.readTree(includeInactive.getResponse().getContentAsString()).get("data").get(0);
    assertFalse(entry.get("active").asBoolean());
  }

  private void assertCustomerCount(MvcResult result, int expected) throws Exception {
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    assertEquals(expected, body.get("data").size());
  }

  private MvcResult fetchCustomerPage(UUID tenantId, boolean includeInactive) throws Exception {
    var request = get("/api/v1/customers")
      .header(HttpHeaders.AUTHORIZATION, token())
      .header(TenantInterceptor.TENANT_HEADER, tenantId.toString());
    if (includeInactive) {
      request.param("includeInactive", "true");
    }
    return mockMvc.perform(request)
      .andExpect(status().isOk())
      .andReturn();
  }

  private Customer createCustomer(UUID tenantId, String name) {
    return runAsTenant(tenantId, () -> customerService.create(customerRequest(name)));
  }

  private UserAccount createAdminAccount(UUID tenantId) {
    UserAccount account = new UserAccount();
    account.setId(UUID.randomUUID());
    account.setCompanyId(tenantId);
    account.setEmail("admin@example.com");
    account.setName("Integration Admin");
    account.setRole("ADMIN");
    account.setStatus("active");
    account.setPasswordHash("secret");
    account.setRoles("ROLE_ADMIN");
    return userAccountRepository.save(account);
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

  private <T> T runAsTenant(UUID tenantId, Supplier<T> action) {
    try {
      TenantContext.setTenantId(tenantId);
      return action.get();
    } finally {
      TenantContext.clear();
    }
  }

  private String token() {
    return "Bearer " + adminToken;
  }

  @TestConfiguration
  @EnableCaching
  public static class CacheTestConfig {

    @Bean
    CacheManager cacheManager() {
      return new ConcurrentMapCacheManager("customers");
    }
  }
}
