package com.datakomerz.pymes.finances;

import static org.assertj.core.api.Assertions.*;

import com.datakomerz.pymes.customers.CustomerRepository;
import com.datakomerz.pymes.finances.dto.FinanceSummary;
import com.datakomerz.pymes.finances.dto.PaymentBucketSummary;
import com.datakomerz.pymes.purchases.Purchase;
import com.datakomerz.pymes.purchases.PurchaseRepository;
import com.datakomerz.pymes.sales.Sale;
import com.datakomerz.pymes.sales.SaleRepository;
import com.datakomerz.pymes.suppliers.SupplierRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class FinanceServiceBucketsTest {
  
  @Mock
  private SaleRepository saleRepository;
  
  @Mock
  private PurchaseRepository purchaseRepository;
  
  @Mock
  private CustomerRepository customerRepository;
  
  @Mock
  private SupplierRepository supplierRepository;
  
  private FinanceService financeService;
  
  @BeforeEach
  void setUp() {
    financeService = new FinanceService(
        saleRepository,
        purchaseRepository,
        customerRepository,
        supplierRepository
    );
  }
  
  @Test
  void getSummary_shouldReturnCorrectReceivableBuckets() {
    // Arrange
    OffsetDateTime now = OffsetDateTime.now();
    
    // Venta vencida (overdue)
    Sale overdueSale = createSale(now.minusDays(40), 30, new BigDecimal("1000"), "pending");
    
    // Venta próxima a vencer (0-7 días)
    Sale dueSoonSale = createSale(now.minusDays(26), 30, new BigDecimal("2000"), "pending");
    
    // Venta 8-15 días
    Sale sale8to15 = createSale(now.minusDays(20), 30, new BigDecimal("3000"), "pending");
    
    // Venta 16-30 días
    Sale sale16to30 = createSale(now.minusDays(10), 30, new BigDecimal("4000"), "pending");
    
    // Venta 31-60 días
    Sale sale31to60 = createSale(now.minusDays(5), 60, new BigDecimal("5000"), "pending");
    
    // Venta 60+ días
    Sale sale60plus = createSale(now, 90, new BigDecimal("6000"), "pending");
    
    List<Sale> sales = List.of(overdueSale, dueSoonSale, sale8to15, sale16to30, sale31to60, sale60plus);
    
    Mockito.when(saleRepository.findAllByOrderByIssuedAtDesc(Mockito.any(Pageable.class)))
        .thenReturn(new PageImpl<>(sales));
    
    Mockito.when(purchaseRepository.findAllByOrderByIssuedAtDesc(Mockito.any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));
    
    // Act
    FinanceSummary summary = financeService.getSummary();
    
    // Assert
    assertThat(summary.receivableBuckets()).hasSize(6);
    
    // Verificar bucket vencido
    PaymentBucketSummary overdueBucket = findBucket(summary.receivableBuckets(), "overdue");
    assertThat(overdueBucket.amount()).isEqualByComparingTo("1000");
    assertThat(overdueBucket.documents()).isEqualTo(1);
    
    // Verificar bucket 0-7 días
    PaymentBucketSummary days0to7 = findBucket(summary.receivableBuckets(), "days_0_7");
    assertThat(days0to7.amount()).isEqualByComparingTo("2000");
    assertThat(days0to7.documents()).isEqualTo(1);
    
    // Verificar bucket 8-15 días
    PaymentBucketSummary days8to15 = findBucket(summary.receivableBuckets(), "days_8_15");
    assertThat(days8to15.amount()).isEqualByComparingTo("3000");
    assertThat(days8to15.documents()).isEqualTo(1);
    
    // Verificar bucket 16-30 días
    PaymentBucketSummary days16to30 = findBucket(summary.receivableBuckets(), "days_16_30");
    assertThat(days16to30.amount()).isEqualByComparingTo("4000");
    assertThat(days16to30.documents()).isEqualTo(1);
    
    // Verificar bucket 31-60 días
    PaymentBucketSummary days31to60 = findBucket(summary.receivableBuckets(), "days_31_60");
    assertThat(days31to60.amount()).isEqualByComparingTo("5000");
    assertThat(days31to60.documents()).isEqualTo(1);
    
    // Verificar bucket 60+ días
    PaymentBucketSummary days60plus = findBucket(summary.receivableBuckets(), "days_60_plus");
    assertThat(days60plus.amount()).isEqualByComparingTo("6000");
    assertThat(days60plus.documents()).isEqualTo(1);
  }
  
  @Test
  void getSummary_shouldReturnCorrectPayableBuckets() {
    // Arrange
    OffsetDateTime now = OffsetDateTime.now();
    
    // Compra vencida
    Purchase overduePurchase = createPurchase(now.minusDays(40), 30, new BigDecimal("500"), "pending");
    
    // Compra próxima a vencer
    Purchase dueSoonPurchase = createPurchase(now.minusDays(25), 30, new BigDecimal("1500"), "pending");
    
    List<Purchase> purchases = List.of(overduePurchase, dueSoonPurchase);
    
    Mockito.when(saleRepository.findAllByOrderByIssuedAtDesc(Mockito.any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));
    
    Mockito.when(purchaseRepository.findAllByOrderByIssuedAtDesc(Mockito.any(Pageable.class)))
        .thenReturn(new PageImpl<>(purchases));
    
    // Act
    FinanceSummary summary = financeService.getSummary();
    
    // Assert
    assertThat(summary.payableBuckets()).hasSize(6);
    
    PaymentBucketSummary overdueBucket = findBucket(summary.payableBuckets(), "overdue");
    assertThat(overdueBucket.amount()).isEqualByComparingTo("500");
    assertThat(overdueBucket.documents()).isEqualTo(1);
    
    PaymentBucketSummary days0to7 = findBucket(summary.payableBuckets(), "days_0_7");
    assertThat(days0to7.amount()).isEqualByComparingTo("1500");
    assertThat(days0to7.documents()).isEqualTo(1);
  }
  
  @Test
  void getSummary_shouldExcludeCancelledDocuments() {
    // Arrange
    OffsetDateTime now = OffsetDateTime.now();
    
    Sale pendingSale = createSale(now, 30, new BigDecimal("1000"), "pending");
    Sale cancelledSale = createSale(now, 30, new BigDecimal("9999"), "cancelled");
    
    List<Sale> sales = List.of(pendingSale, cancelledSale);
    
    Mockito.when(saleRepository.findAllByOrderByIssuedAtDesc(Mockito.any(Pageable.class)))
        .thenReturn(new PageImpl<>(sales));
    
    Mockito.when(purchaseRepository.findAllByOrderByIssuedAtDesc(Mockito.any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));
    
    // Act
    FinanceSummary summary = financeService.getSummary();
    
    // Assert
    BigDecimal totalInBuckets = summary.receivableBuckets().stream()
        .map(PaymentBucketSummary::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    assertThat(totalInBuckets).isEqualByComparingTo("1000");
    assertThat(summary.totalReceivables()).isEqualByComparingTo("1000");
  }
  
  @Test
  void getSummary_shouldHandleEmptySales() {
    // Arrange
    Mockito.when(saleRepository.findAllByOrderByIssuedAtDesc(Mockito.any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));
    
    Mockito.when(purchaseRepository.findAllByOrderByIssuedAtDesc(Mockito.any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));
    
    // Act
    FinanceSummary summary = financeService.getSummary();
    
    // Assert
    assertThat(summary.receivableBuckets()).hasSize(6);
    assertThat(summary.payableBuckets()).hasSize(6);
    
    summary.receivableBuckets().forEach(bucket -> {
      assertThat(bucket.amount()).isEqualByComparingTo("0");
      assertThat(bucket.documents()).isEqualTo(0);
    });
  }
  
  private Sale createSale(OffsetDateTime issuedAt, int paymentTermDays, BigDecimal total, String status) {
    Sale sale = new Sale();
    sale.setId(UUID.randomUUID());
    sale.setIssuedAt(issuedAt);
    sale.setPaymentTermDays(paymentTermDays);
    sale.setTotal(total);
    sale.setNet(total.multiply(new BigDecimal("0.81")));
    sale.setVat(total.multiply(new BigDecimal("0.19")));
    sale.setStatus(status);
    sale.setDocType("BOLETA");
    sale.setPaymentMethod("EFECTIVO");
    return sale;
  }
  
  private Purchase createPurchase(OffsetDateTime issuedAt, int paymentTermDays, BigDecimal total, String status) {
    Purchase purchase = new Purchase();
    purchase.setId(UUID.randomUUID());
    purchase.setIssuedAt(issuedAt);
    purchase.setPaymentTermDays(paymentTermDays);
    purchase.setTotal(total);
    purchase.setNet(total.multiply(new BigDecimal("0.81")));
    purchase.setVat(total.multiply(new BigDecimal("0.19")));
    purchase.setStatus(status);
    purchase.setDocType("FACTURA");
    purchase.setDocNumber("12345");
    return purchase;
  }
  
  private PaymentBucketSummary findBucket(List<PaymentBucketSummary> buckets, String key) {
    return buckets.stream()
        .filter(b -> b.key().equals(key))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Bucket not found: " + key));
  }
}
