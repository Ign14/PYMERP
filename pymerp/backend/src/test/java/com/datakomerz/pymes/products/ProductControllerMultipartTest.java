package com.datakomerz.pymes.products;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.datakomerz.pymes.config.TestJwtDecoderConfig;
import com.datakomerz.pymes.inventory.InventoryService;
import com.datakomerz.pymes.pricing.PricingService;
import com.datakomerz.pymes.storage.StorageService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestJwtDecoderConfig.class)
class ProductControllerMultipartTest {
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

  @Test
  void rejectsUnsupportedImageFormat() throws Exception {
    when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
      Product product = invocation.getArgument(0);
      if (product.getId() == null) {
        product.setId(UUID.randomUUID());
      }
      product.setCriticalStock(BigDecimal.ZERO);
      return product;
    });

    MockMultipartFile payload = new MockMultipartFile(
      "product",
      "product.json",
      MediaType.APPLICATION_JSON_VALUE,
      (
        "{" +
          "\"sku\":\"SKU-001\"," +
          "\"name\":\"Demo\"," +
          "\"description\":\"Test\"," +
          "\"category\":\"Cat\"," +
          "\"barcode\":\"123\"" +
        "}"
      ).getBytes(StandardCharsets.UTF_8)
    );

    MockMultipartFile invalidImage = new MockMultipartFile(
      "image",
      "demo.gif",
      MediaType.IMAGE_GIF_VALUE,
      "gif".getBytes(StandardCharsets.UTF_8)
    );

    mockMvc.perform(multipart("/api/v1/products")
        .file(payload)
        .file(invalidImage)
        .with(jwt().jwt(jwt -> jwt.claim("realm_access", Map.of("roles", List.of("erp_user"))))
          .authorities(new SimpleGrantedAuthority("ROLE_ERP_USER")))
        .header("X-Company-Id", COMPANY_ID))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.detail", Matchers.containsString("Unsupported image format")));

    verify(storageService, never()).storeProductImage(any(), any(), any());
  }
}
