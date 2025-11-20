package com.datakomerz.pymes.billing.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.datakomerz.pymes.auth.UserAccount;
import com.datakomerz.pymes.auth.UserAccountRepository;
import com.datakomerz.pymes.billing.dto.IssueInvoiceRequest;
import com.datakomerz.pymes.billing.dto.InvoiceSaleDto;
import com.datakomerz.pymes.billing.dto.InvoiceSaleItemDto;
import com.datakomerz.pymes.config.TestJwtDecoderConfig;
import com.datakomerz.pymes.company.Company;
import com.datakomerz.pymes.company.CompanyRepository;
import com.datakomerz.pymes.integration.AbstractIT;
import com.datakomerz.pymes.multitenancy.TenantContext;
import com.datakomerz.pymes.multitenancy.TenantInterceptor;
import com.datakomerz.pymes.sales.Sale;
import com.datakomerz.pymes.sales.SaleRepository;
import com.datakomerz.pymes.security.AppUserDetails;
import com.datakomerz.pymes.security.jwt.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
class BillingIdempotencyIT extends AbstractIT {

  private static final String BASE_TOKEN_PREFIX = "Bearer ";

  @DynamicPropertySource
  static void overrideProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.autoconfigure.exclude", () -> "");
    registry.add("app.cache.redis.enabled", () -> "true");
  }

  @LocalServerPort
  private int port;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private SaleRepository saleRepository;

  @Autowired
  private CompanyRepository companyRepository;

  @Autowired
  private UserAccountRepository userAccountRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private JwtService jwtService;

  @Autowired
  private StringRedisTemplate redisTemplate;

  private UUID tenantId;
  private UUID saleId;
  private String token;

  @BeforeEach
  void setUp() {
    jdbcTemplate.execute("TRUNCATE TABLE document_files, contingency_queue_items, fiscal_documents, sales, users, companies CASCADE;");
    redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
      connection.flushDb();
      return null;
    });
    tenantId = UUID.randomUUID();
    companyRepository.save(composeCompany(tenantId));
    saleId = createSaleForTenant(tenantId);
    token = buildTokenForTenant(tenantId);
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;
  }

  @Test
  void happyPath_returnsCreatedThenReusesInvoice() throws Exception {
    String idempotencyKey = "happy-invoice";
    IssueInvoiceRequest request = invoiceRequest(saleId, idempotencyKey, "Item A");
    String payload = objectMapper.writeValueAsString(request);

    Response first = sendInvoiceRequest(payload, idempotencyKey, tenantId, token);
    first.then().statusCode(201);
    String invoiceId = first.jsonPath().getString("id");

    Response second = sendInvoiceRequest(payload, idempotencyKey, tenantId, token);
    second.then()
        .statusCode(200)
        .body("id", equalTo(invoiceId));
  }

  @Test
  void conflict_whenPayloadDiffers() throws Exception {
    String idempotencyKey = "mismatch-invoice";
    String basePayload = objectMapper.writeValueAsString(invoiceRequest(saleId, idempotencyKey, "Item A"));
    sendInvoiceRequest(basePayload, idempotencyKey, tenantId, token).then().statusCode(201);

    String differentPayload = objectMapper.writeValueAsString(invoiceRequest(saleId, idempotencyKey, "Item B"));
    sendInvoiceRequest(differentPayload, idempotencyKey, tenantId, token)
        .then()
        .statusCode(409)
        .body("detail", equalTo("Idempotency-Key reused with a different payload"));
  }

  @Test
  void concurrentRequests_shareSingleInvoice() throws Exception {
    String idempotencyKey = "concurrent-key";
    String payload = objectMapper.writeValueAsString(invoiceRequest(saleId, idempotencyKey, "Concurrent Item"));

    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Response>> futures = new ArrayList<>();
    futures.add(executor.submit(() -> latchAndSend(payload, idempotencyKey, ready, start, tenantId, token)));
    futures.add(executor.submit(() -> latchAndSend(payload, idempotencyKey, ready, start, tenantId, token)));

    ready.await(5, TimeUnit.SECONDS);
    start.countDown();

    Set<String> invoiceIds = new HashSet<>();
    List<Integer> statuses = new ArrayList<>();
    for (Future<Response> future : futures) {
      Response response = future.get(15, TimeUnit.SECONDS);
      statuses.add(response.getStatusCode());
      invoiceIds.add(response.jsonPath().getString("id"));
    }
    executor.shutdownNow();

    assertThat(statuses).contains(201, 200);
    assertThat(invoiceIds).hasSize(1);
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM fiscal_documents WHERE company_id = ?",
        Integer.class, tenantId);
    assertThat(count).isEqualTo(1);
  }

  @Test
  void sameKey_differentTenants() throws Exception {
    String sharedKey = "shared-key";
    String payloadA = objectMapper.writeValueAsString(invoiceRequest(saleId, sharedKey, "Tenant A Item"));
    sendInvoiceRequest(payloadA, sharedKey, tenantId, token).then().statusCode(201);

    UUID tenantB = UUID.randomUUID();
    companyRepository.save(composeCompany(tenantB));
    UUID saleB = createSaleForTenant(tenantB);
    String tokenB = buildTokenForTenant(tenantB);
    String payloadB = objectMapper.writeValueAsString(invoiceRequest(saleB, sharedKey, "Tenant B Item"));

    sendInvoiceRequest(payloadB, sharedKey, tenantB, tokenB).then().statusCode(201);
  }

  private Response latchAndSend(String payload,
                                String key,
                                CountDownLatch ready,
                                CountDownLatch start,
                                UUID tenant,
                                String authToken) throws Exception {
    ready.countDown();
    start.await();
    return sendInvoiceRequest(payload, key, tenant, authToken);
  }

  private Response sendInvoiceRequest(String payload,
                                      String idempotencyKey,
                                      UUID tenant,
                                      String authToken) {
    return RestAssured.given()
        .contentType(ContentType.JSON)
        .header(HttpHeaders.AUTHORIZATION, authToken)
        .header(TenantInterceptor.TENANT_HEADER, tenant.toString())
        .header("Idempotency-Key", idempotencyKey)
        .body(payload)
        .when()
        .post("/api/v1/billing/invoices");
  }

  private IssueInvoiceRequest invoiceRequest(UUID saleId, String idempotencyKey, String description) {
    InvoiceSaleItemDto item = new InvoiceSaleItemDto(
        UUID.randomUUID(),
        description,
        BigDecimal.ONE,
        new BigDecimal("100"),
        BigDecimal.ZERO);
    InvoiceSaleDto saleDto = new InvoiceSaleDto(
        saleId,
        List.of(item),
        new BigDecimal("100"),
        new BigDecimal("19"),
        new BigDecimal("119"),
        "Integration Customer",
        "12345678-9",
        "POS-1",
        "DEVICE-1");
    return new IssueInvoiceRequest(
        com.company.billing.persistence.FiscalDocumentType.FACTURA,
        com.company.billing.persistence.TaxMode.AFECTA,
        saleDto,
        idempotencyKey,
        true,
        "GOOD");
  }

  private Company composeCompany(UUID tenant) {
    Company company = new Company();
    company.setId(tenant);
    company.setBusinessName("Tenant " + tenant);
    company.setRut(tenant.toString().replace("-", "").substring(0, 12));
    company.setBusinessActivity("integration");
    return company;
  }

  private UUID createSaleForTenant(UUID tenant) {
    Sale sale = new Sale();
    sale.setCompanyId(tenant);
    sale.setStatus("READY");
    sale.setNet(new BigDecimal("100"));
    sale.setVat(new BigDecimal("19"));
    sale.setTotal(new BigDecimal("119"));
    sale.setPaymentMethod("CARD");
    sale.setDocType("FACTURA");
    sale.setIssuedAt(OffsetDateTime.now());
    return runAsTenant(tenant, () -> saleRepository.save(sale)).getId();
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
    account.setRoles("ROLE_ERP_USER");
    UserAccount saved = userAccountRepository.save(account);
    return BASE_TOKEN_PREFIX + jwtService.generateToken(new AppUserDetails(saved));
  }

  private <T> T runAsTenant(UUID tenant, java.util.concurrent.Callable<T> action) {
    try {
      TenantContext.setTenantId(tenant);
      return action.call();
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    } finally {
      TenantContext.clear();
    }
  }
}
