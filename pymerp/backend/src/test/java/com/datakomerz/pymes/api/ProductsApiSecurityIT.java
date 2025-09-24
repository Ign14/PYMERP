package com.datakomerz.pymes.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.web.servlet.MockMvc;
import com.datakomerz.pymes.products.ProductRepository;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductsApiSecurityIT {

  @Autowired
  MockMvc mockMvc;

  @MockBean
  ProductRepository productRepository;

  private static final String COMPANY_HEADER = "X-Company-Id";
  private static final String COMPANY_ID = "00000000-0000-0000-0000-000000000001";

  @Test
  void returns200_when_tokenHasRole_or_scope_and_companyHeaderPresent() throws Exception {
    // return empty page so controller doesn't hit DB problems
    Mockito.when(productRepository.findByCompanyIdAndDeletedAtIsNullAndNameContainingIgnoreCase(Mockito.any(), Mockito.anyString(), Mockito.any()))
      .thenReturn(new PageImpl<>(List.of()));
  // with realm role
  mockMvc.perform(
    get("/api/v1/products")
      .header(COMPANY_HEADER, COMPANY_ID)
  .with(jwt().jwt(jwt -> jwt.claim("realm_access", java.util.Map.of("roles", java.util.List.of("erp_user")))))
  ).andExpect(status().isOk());

  // with scope
  mockMvc.perform(
    get("/api/v1/products")
      .header(COMPANY_HEADER, COMPANY_ID)
  .with(jwt().jwt(jwt -> jwt.claim("scope", "products:read")))
  ).andExpect(status().isOk());
  }

  @Test
  void returns403_when_tokenWithoutRoleOrScope() throws Exception {
  mockMvc.perform(get("/api/v1/products").header(COMPANY_HEADER, COMPANY_ID)
    .with(jwt()))
      .andExpect(status().isForbidden());
  }

  @Test
  void returns400_when_missingCompanyHeader() throws Exception {
  mockMvc.perform(get("/api/v1/products")
    .with(jwt().jwt(jwt -> jwt.claim("realm_access", java.util.Map.of("roles", java.util.List.of("erp_user"))))))
      .andExpect(status().isBadRequest());
  }

  @Test
  void returns401_when_noTokenProvided() throws Exception {
    mockMvc.perform(get("/api/v1/products").header(COMPANY_HEADER, COMPANY_ID))
      .andExpect(status().isUnauthorized());
  }
}
