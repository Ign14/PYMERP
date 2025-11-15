package com.datakomerz.pymes.inventory.integration;

import com.datakomerz.pymes.auth.UserAccount;
import com.datakomerz.pymes.auth.UserAccountRepository;
import com.datakomerz.pymes.company.Company;
import com.datakomerz.pymes.company.CompanyRepository;
import com.datakomerz.pymes.integration.AbstractIT;
import com.datakomerz.pymes.multitenancy.TenantInterceptor;
import com.datakomerz.pymes.security.AppUserDetails;
import com.datakomerz.pymes.security.jwt.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;

public abstract class AbstractInventoryApiIT extends AbstractIT {

  @Autowired
  protected JwtService jwtService;

  @Autowired
  protected CompanyRepository companyRepository;

  @Autowired
  protected UserAccountRepository userAccountRepository;

  @Autowired
  protected ObjectMapper objectMapper;

  @Autowired
  protected JdbcTemplate jdbcTemplate;

  protected Company createCompany(UUID companyId, String name) {
    Company company = new Company();
    company.setId(companyId);
    company.setBusinessName(name);
    company.setRut(companyId.toString().replace("-", "").substring(0, 8) + "-1");
    return companyRepository.save(company);
  }

  protected String bearerToken(UUID companyId, String email) {
    UserAccount account = new UserAccount();
    account.setCompanyId(companyId);
    account.setEmail(email);
    account.setName("Integration Admin");
    account.setRoles("ROLE_ADMIN,ROLE_ERP_USER");
    account.setPasswordHash("test-password");
    account = userAccountRepository.save(account);
    return "Bearer " + jwtService.generateToken(new AppUserDetails(account));
  }

  protected RequestSpecification authenticated(String token, UUID companyId) {
    return RestAssured.given()
        .contentType(ContentType.JSON)
        .header(HttpHeaders.AUTHORIZATION, token)
        .header(TenantInterceptor.TENANT_HEADER, companyId.toString());
  }

  protected void truncateTables(String... tables) {
    for (String table : tables) {
      jdbcTemplate.execute("TRUNCATE TABLE " + table + " RESTART IDENTITY CASCADE;");
    }
  }
}
