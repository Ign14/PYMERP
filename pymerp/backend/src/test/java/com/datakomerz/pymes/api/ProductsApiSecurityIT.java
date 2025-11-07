package com.datakomerz.pymes.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.datakomerz.pymes.config.TestJwtDecoderConfig;
import com.datakomerz.pymes.products.ProductRepository;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestJwtDecoderConfig.class)
public class ProductsApiSecurityIT {

  @Autowired
  MockMvc mockMvc;

  @MockBean
  ProductRepository productRepository;

  private static final String COMPANY_HEADER = "X-Company-Id";
  private static final String COMPANY_ID = "00000000-0000-0000-0000-000000000001";
  private static final Collection<GrantedAuthority> ROLE_AUTHORITY =
      List.of(new SimpleGrantedAuthority("ROLE_ERP_USER"));
  private static final Collection<GrantedAuthority> SCOPE_AUTHORITY =
      List.of(new SimpleGrantedAuthority("SCOPE_products:read"));
  private static final Collection<GrantedAuthority> NO_AUTHORITIES = Collections.<GrantedAuthority>emptyList();

  @Test
  void returns200_when_tokenHasRole_or_scope_and_companyHeaderPresent() throws Exception {
    Mockito.when(productRepository.findByDeletedAtIsNullAndNameContainingIgnoreCase(
            Mockito.anyString(), Mockito.any()))
        .thenReturn(new PageImpl<>(List.of()));
    Mockito.when(productRepository.findByDeletedAtIsNullAndActiveIsAndNameContainingIgnoreCase(
            Mockito.anyBoolean(), Mockito.anyString(), Mockito.any()))
        .thenReturn(new PageImpl<>(List.of()));
    Mockito.when(productRepository.findByDeletedAtIsNullAndActiveIsTrue(Mockito.any()))
        .thenReturn(new PageImpl<>(List.of()));

    // with realm role
    mockMvc.perform(get("/api/v1/products")
            .header(COMPANY_HEADER, COMPANY_ID)
            .with(jwt().jwt(jwt -> jwt.claim("realm_access",
                java.util.Map.of("roles", java.util.List.of("erp_user"))))
              .authorities(ROLE_AUTHORITY)))
        .andExpect(status().isOk());

    // with scope
    mockMvc.perform(get("/api/v1/products")
            .header(COMPANY_HEADER, COMPANY_ID)
            .with(jwt().jwt(jwt -> jwt.claim("scope", "products:read"))
              .authorities(SCOPE_AUTHORITY)))
        .andExpect(status().isOk());
  }

  @Test
  void returns403_when_tokenWithoutRoleOrScope() throws Exception {
    mockMvc.perform(get("/api/v1/products")
            .header(COMPANY_HEADER, COMPANY_ID)
            .with(jwt().authorities(NO_AUTHORITIES)))
        .andExpect(status().isForbidden());
  }

  @Test
  void returns400_when_missingCompanyHeader() throws Exception {
    mockMvc.perform(get("/api/v1/products")
            .with(jwt().jwt(jwt -> jwt.claim("realm_access",
                java.util.Map.of("roles", java.util.List.of("erp_user"))))
              .authorities(ROLE_AUTHORITY)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void returns401_when_noTokenProvided() throws Exception {
    mockMvc.perform(get("/api/v1/products")
            .header(COMPANY_HEADER, COMPANY_ID))
        .andExpect(status().isUnauthorized());
  }
}
