package com.datakomerz.pymes.products;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.datakomerz.pymes.config.TestJwtDecoderConfig;
import com.datakomerz.pymes.inventory.InventoryService;
import com.datakomerz.pymes.multitenancy.TenantValidationAspect;
import com.datakomerz.pymes.pricing.PricingService;
import com.datakomerz.pymes.storage.StorageService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestJwtDecoderConfig.class)
class ProductInventoryAlertControllerTest {
  private static final String COMPANY_ID = "00000000-0000-0000-0000-000000000001";

  @Autowired
  MockMvc mockMvc;

  @MockBean
  ProductRepository productRepository;

  @MockBean
  PricingService pricingService;

  @MockBean
  StorageService storageService;

  @MockBean
  QrCodeService qrCodeService;

  @MockBean
  InventoryService inventoryService;

  @MockBean
  TenantValidationAspect tenantValidationAspect;

  @Test
  void updatesCriticalStock() throws Exception {
    UUID productId = UUID.randomUUID();
    Product product = new Product();
    product.setId(productId);
    product.setCompanyId(UUID.fromString(COMPANY_ID));
    product.setSku("SKU-001");
    product.setName("Demo");
    product.setCriticalStock(BigDecimal.ZERO);
    product.setActive(Boolean.TRUE);

    when(productRepository.findById(productId)).thenReturn(Optional.of(product));
    when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(pricingService.latestPrice(productId)).thenReturn(Optional.empty());

    mockMvc.perform(patch("/api/v1/products/{id}/inventory-alert", productId)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"criticalStock\":5}")
        .with(jwt().jwt(jwt -> jwt.claim("realm_access", Map.of("roles", List.of("settings"))))
          .authorities(new SimpleGrantedAuthority("ROLE_SETTINGS")))
        .header("X-Company-Id", COMPANY_ID))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.criticalStock").value(5));
  }
}
