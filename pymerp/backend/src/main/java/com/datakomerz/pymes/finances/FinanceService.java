package com.datakomerz.pymes.finances;

import com.datakomerz.pymes.customers.CustomerRepository;
import com.datakomerz.pymes.finances.dto.AccountPayable;
import com.datakomerz.pymes.finances.dto.AccountReceivable;
import com.datakomerz.pymes.finances.dto.CashflowProjection;
import com.datakomerz.pymes.finances.dto.FinanceSummary;
import com.datakomerz.pymes.finances.dto.PaymentBucketSummary;
import com.datakomerz.pymes.purchases.Purchase;
import com.datakomerz.pymes.purchases.PurchaseRepository;
import com.datakomerz.pymes.sales.Sale;
import com.datakomerz.pymes.sales.SaleRepository;
import com.datakomerz.pymes.suppliers.SupplierRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceService {
  
  private final SaleRepository saleRepository;
  private final PurchaseRepository purchaseRepository;
  private final CustomerRepository customerRepository;
  private final SupplierRepository supplierRepository;

  public FinanceService(
      SaleRepository saleRepository,
      PurchaseRepository purchaseRepository,
      CustomerRepository customerRepository,
      SupplierRepository supplierRepository) {
    this.saleRepository = saleRepository;
    this.purchaseRepository = purchaseRepository;
    this.customerRepository = customerRepository;
    this.supplierRepository = supplierRepository;
  }

  @Transactional(readOnly = true)
  public FinanceSummary getSummary() {
    // Obtener todas las ventas pendientes de pago (excluyendo canceladas)
    List<Sale> pendingSales = saleRepository.findAllByOrderByIssuedAtDesc(Pageable.unpaged())
        .getContent()
        .stream()
        .filter(s -> s.getStatus() != null && !"cancelled".equalsIgnoreCase(s.getStatus()))
        .toList();
    
    // Obtener todas las compras pendientes de pago (excluyendo canceladas)
    List<Purchase> pendingPurchases = purchaseRepository.findAllByOrderByIssuedAtDesc(Pageable.unpaged())
        .getContent()
        .stream()
        .filter(p -> p.getStatus() != null && !"cancelled".equalsIgnoreCase(p.getStatus()))
        .toList();
    
    OffsetDateTime now = OffsetDateTime.now();
    OffsetDateTime next7Days = now.plusDays(7);
    OffsetDateTime next30Days = now.plusDays(30);
    
    // Calcular cuentas por cobrar
    BigDecimal totalReceivable = pendingSales.stream()
        .map(s -> s.getTotal() != null ? s.getTotal() : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    long overdueInvoices = pendingSales.stream()
        .filter(s -> s.getIssuedAt() != null && s.getDueDate().isBefore(now))
        .count();
    
    BigDecimal next7DaysReceivable = pendingSales.stream()
        .filter(s -> s.getIssuedAt() != null && s.getDueDate().isBefore(next7Days))
        .map(s -> s.getTotal() != null ? s.getTotal() : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal next30DaysReceivable = pendingSales.stream()
        .filter(s -> s.getIssuedAt() != null && s.getDueDate().isBefore(next30Days))
        .map(s -> s.getTotal() != null ? s.getTotal() : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    // Calcular cuentas por pagar
    BigDecimal totalPayable = pendingPurchases.stream()
        .map(p -> p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    long overduePurchases = pendingPurchases.stream()
        .filter(p -> p.getIssuedAt() != null && p.getDueDate().isBefore(now))
        .count();
    
    BigDecimal next7DaysPayable = pendingPurchases.stream()
        .filter(p -> p.getIssuedAt() != null && p.getDueDate().isBefore(next7Days))
        .map(p -> p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal next30DaysPayable = pendingPurchases.stream()
        .filter(p -> p.getIssuedAt() != null && p.getDueDate().isBefore(next30Days))
        .map(p -> p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    // Posición neta (sin caja física por ahora)
    BigDecimal netPosition = totalReceivable.subtract(totalPayable);
    
    // Calcular buckets de antigüedad
    List<PaymentBucketSummary> receivableBuckets = calculateReceivableBuckets(pendingSales, now);
    List<PaymentBucketSummary> payableBuckets = calculatePayableBuckets(pendingPurchases, now);
    
    return new FinanceSummary(
        BigDecimal.ZERO, // totalCash - requiere módulo de tesorería
        totalReceivable,
        totalPayable,
        netPosition,
        pendingSales.size(),
        overdueInvoices,
        pendingPurchases.size(),
        overduePurchases,
        next7DaysReceivable,
        next7DaysPayable,
        next30DaysReceivable,
        next30DaysPayable,
        receivableBuckets,
        payableBuckets
    );
  }

  @Transactional(readOnly = true)
  public Page<AccountReceivable> getAccountsReceivable(String status, Pageable pageable) {
    List<Sale> sales = saleRepository.findAllByOrderByIssuedAtDesc(Pageable.unpaged())
        .getContent()
        .stream()
        .filter(s -> s.getStatus() != null && !"cancelled".equalsIgnoreCase(s.getStatus()))
        .toList();
    
    // Obtener nombres de clientes
    List<UUID> customerIds = sales.stream()
        .map(Sale::getCustomerId)
        .filter(id -> id != null)
        .distinct()
        .toList();
    
    Map<UUID, String> customerNames = customerRepository.findAllById(customerIds).stream()
        .collect(Collectors.toMap(
            c -> c.getId(),
            c -> c.getName()
        ));
    
    OffsetDateTime now = OffsetDateTime.now();
    
    List<AccountReceivable> receivables = sales.stream()
        .map(sale -> {
          OffsetDateTime dueDate = sale.getIssuedAt() != null 
              ? sale.getIssuedAt().plusDays(30) 
              : null;
          
          long daysOverdue = 0;
          String paymentStatus = "PENDING";
          
          if (dueDate != null) {
            if (dueDate.isBefore(now)) {
              daysOverdue = ChronoUnit.DAYS.between(dueDate, now);
              paymentStatus = "OVERDUE";
            } else if (dueDate.isBefore(now.plusDays(7))) {
              paymentStatus = "DUE_SOON";
            }
          }
          
          return new AccountReceivable(
              sale.getId(), // id
              sale.getId(), // saleId
              sale.getCustomerId(),
              sale.getCustomerId() != null 
                  ? customerNames.getOrDefault(sale.getCustomerId(), "Desconocido")
                  : "Cliente directo",
              sale.getDocType(),
              sale.getId().toString(), // Usar ID como número de documento
              sale.getTotal(),
              BigDecimal.ZERO, // paid - requiere módulo de pagos
              sale.getTotal(), // balance = total - paid
              sale.getStatus(),
              sale.getIssuedAt(),
              dueDate,
              daysOverdue,
              paymentStatus,
              sale.getPaymentTermDays()
          );
        })
        .toList();
    
    // Filtrar por estado si se especifica
    List<AccountReceivable> filtered = receivables;
    if (status != null && !status.isEmpty()) {
      filtered = receivables.stream()
          .filter(r -> r.paymentStatus().equalsIgnoreCase(status))
          .toList();
    }
    
    // Paginación manual
    int start = (int) pageable.getOffset();
    int end = Math.min(start + pageable.getPageSize(), filtered.size());
    List<AccountReceivable> pageContent = start < filtered.size() ? filtered.subList(start, end) : List.of();
    
    return new PageImpl<>(pageContent, pageable, filtered.size());
  }

  @Transactional(readOnly = true)
  public Page<AccountPayable> getAccountsPayable(String status, Pageable pageable) {
    List<Purchase> purchases = purchaseRepository.findAllByOrderByIssuedAtDesc(Pageable.unpaged())
        .getContent()
        .stream()
        .filter(p -> p.getStatus() != null && !"cancelled".equalsIgnoreCase(p.getStatus()))
        .toList();
    
    // Obtener nombres de proveedores
    List<UUID> supplierIds = purchases.stream()
        .map(Purchase::getSupplierId)
        .filter(id -> id != null)
        .distinct()
        .toList();
    
    Map<UUID, String> supplierNames = supplierRepository.findAllById(supplierIds).stream()
        .collect(Collectors.toMap(
            s -> s.getId(),
            s -> s.getName()
        ));
    
    OffsetDateTime now = OffsetDateTime.now();
    
    List<AccountPayable> payables = purchases.stream()
        .map(purchase -> {
          OffsetDateTime dueDate = purchase.getIssuedAt() != null 
              ? purchase.getIssuedAt().plusDays(30) 
              : null;
          
          long daysOverdue = 0;
          String paymentStatus = "PENDING";
          
          if (dueDate != null) {
            if (dueDate.isBefore(now)) {
              daysOverdue = ChronoUnit.DAYS.between(dueDate, now);
              paymentStatus = "OVERDUE";
            } else if (dueDate.isBefore(now.plusDays(7))) {
              paymentStatus = "DUE_SOON";
            }
          }
          
          return new AccountPayable(
              purchase.getId(),
              purchase.getId(),
              purchase.getSupplierId(),
              purchase.getSupplierId() != null 
                  ? supplierNames.getOrDefault(purchase.getSupplierId(), "Desconocido")
                  : "Proveedor directo",
              purchase.getDocType(),
              purchase.getDocNumber(),
              purchase.getTotal(),
              BigDecimal.ZERO, // paid - requiere módulo de pagos
              purchase.getTotal(), // balance = total - paid
              purchase.getStatus(),
              purchase.getIssuedAt(),
              dueDate,
              daysOverdue,
              paymentStatus,
              purchase.getPaymentTermDays()
          );
        })
        .toList();
    
    // Filtrar por estado si se especifica
    List<AccountPayable> filtered = payables;
    if (status != null && !status.isEmpty()) {
      filtered = payables.stream()
          .filter(p -> p.paymentStatus().equalsIgnoreCase(status))
          .toList();
    }
    
    // Paginación manual
    int start = (int) pageable.getOffset();
    int end = Math.min(start + pageable.getPageSize(), filtered.size());
    List<AccountPayable> pageContent = start < filtered.size() ? filtered.subList(start, end) : List.of();
    
    return new PageImpl<>(pageContent, pageable, filtered.size());
  }

  @Transactional(readOnly = true)
  public List<CashflowProjection> getCashflowProjection(int days) {
    List<Sale> sales = saleRepository.findAllByOrderByIssuedAtDesc(Pageable.unpaged())
        .getContent()
        .stream()
        .filter(s -> s.getStatus() != null && !"cancelled".equalsIgnoreCase(s.getStatus()))
        .toList();
    
    List<Purchase> purchases = purchaseRepository.findAllByOrderByIssuedAtDesc(Pageable.unpaged())
        .getContent()
        .stream()
        .filter(p -> p.getStatus() != null && !"cancelled".equalsIgnoreCase(p.getStatus()))
        .toList();
    
    LocalDate today = LocalDate.now();
    List<CashflowProjection> projections = new ArrayList<>();
    BigDecimal cumulativeBalance = BigDecimal.ZERO;
    
    for (int i = 0; i < days; i++) {
      LocalDate date = today.plusDays(i);
      OffsetDateTime dayStart = date.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
      OffsetDateTime dayEnd = dayStart.plusDays(1);
      
      // Ingresos esperados (ventas con fecha de vencimiento en este día)
      BigDecimal expectedIncome = sales.stream()
          .filter(s -> s.getIssuedAt() != null)
          .filter(s -> {
            OffsetDateTime dueDate = s.getIssuedAt().plusDays(30);
            return !dueDate.isBefore(dayStart) && dueDate.isBefore(dayEnd);
          })
          .map(s -> s.getTotal() != null ? s.getTotal() : BigDecimal.ZERO)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
      
      // Egresos esperados (compras con fecha de vencimiento en este día)
      BigDecimal expectedExpense = purchases.stream()
          .filter(p -> p.getIssuedAt() != null)
          .filter(p -> {
            OffsetDateTime dueDate = p.getIssuedAt().plusDays(30);
            return !dueDate.isBefore(dayStart) && dueDate.isBefore(dayEnd);
          })
          .map(p -> p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
      
      BigDecimal netFlow = expectedIncome.subtract(expectedExpense);
      cumulativeBalance = cumulativeBalance.add(netFlow);
      
      String period = date.getDayOfWeek().toString().substring(0, 3) + " " + date.getDayOfMonth();
      
      projections.add(new CashflowProjection(
          date,
          expectedIncome,
          expectedExpense,
          netFlow,
          cumulativeBalance,
          period
      ));
    }
    
    return projections;
  }
  
  /**
   * Calcula buckets de antigüedad para cuentas por cobrar.
   * Agrupa ventas por días hasta vencimiento.
   */
  private List<PaymentBucketSummary> calculateReceivableBuckets(List<Sale> sales, OffsetDateTime now) {
    // Definir buckets
    List<BucketDefinition> bucketDefs = List.of(
        new BucketDefinition("overdue", "Vencido", Integer.MIN_VALUE, -1),
        new BucketDefinition("days_0_7", "0-7 días", 0, 7),
        new BucketDefinition("days_8_15", "8-15 días", 8, 15),
        new BucketDefinition("days_16_30", "16-30 días", 16, 30),
        new BucketDefinition("days_31_60", "31-60 días", 31, 60),
        new BucketDefinition("days_60_plus", "60+ días", 61, Integer.MAX_VALUE)
    );
    
    // Agrupar ventas por bucket
    Map<String, List<Sale>> salesByBucket = sales.stream()
        .collect(Collectors.groupingBy(sale -> {
          if (sale.getIssuedAt() == null) return "days_0_7"; // default
          long daysUntilDue = ChronoUnit.DAYS.between(now, sale.getDueDate());
          for (BucketDefinition def : bucketDefs) {
            if (daysUntilDue >= def.minDays && daysUntilDue <= def.maxDays) {
              return def.key;
            }
          }
          return "days_60_plus"; // fallback
        }));
    
    // Crear PaymentBucketSummary para cada bucket
    return bucketDefs.stream()
        .map(def -> {
          List<Sale> bucketSales = salesByBucket.getOrDefault(def.key, List.of());
          BigDecimal amount = bucketSales.stream()
              .map(s -> s.getTotal() != null ? s.getTotal() : BigDecimal.ZERO)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
          return new PaymentBucketSummary(
              def.key,
              def.label,
              def.minDays,
              def.maxDays,
              amount,
              bucketSales.size()
          );
        })
        .toList();
  }
  
  /**
   * Calcula buckets de antigüedad para cuentas por pagar.
   * Agrupa compras por días hasta vencimiento.
   */
  private List<PaymentBucketSummary> calculatePayableBuckets(List<Purchase> purchases, OffsetDateTime now) {
    // Definir buckets
    List<BucketDefinition> bucketDefs = List.of(
        new BucketDefinition("overdue", "Vencido", Integer.MIN_VALUE, -1),
        new BucketDefinition("days_0_7", "0-7 días", 0, 7),
        new BucketDefinition("days_8_15", "8-15 días", 8, 15),
        new BucketDefinition("days_16_30", "16-30 días", 16, 30),
        new BucketDefinition("days_31_60", "31-60 días", 31, 60),
        new BucketDefinition("days_60_plus", "60+ días", 61, Integer.MAX_VALUE)
    );
    
    // Agrupar compras por bucket
    Map<String, List<Purchase>> purchasesByBucket = purchases.stream()
        .collect(Collectors.groupingBy(purchase -> {
          if (purchase.getIssuedAt() == null) return "days_0_7"; // default
          long daysUntilDue = ChronoUnit.DAYS.between(now, purchase.getDueDate());
          for (BucketDefinition def : bucketDefs) {
            if (daysUntilDue >= def.minDays && daysUntilDue <= def.maxDays) {
              return def.key;
            }
          }
          return "days_60_plus"; // fallback
        }));
    
    // Crear PaymentBucketSummary para cada bucket
    return bucketDefs.stream()
        .map(def -> {
          List<Purchase> bucketPurchases = purchasesByBucket.getOrDefault(def.key, List.of());
          BigDecimal amount = bucketPurchases.stream()
              .map(p -> p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
          return new PaymentBucketSummary(
              def.key,
              def.label,
              def.minDays,
              def.maxDays,
              amount,
              bucketPurchases.size()
          );
        })
        .toList();
  }
  
  /**
   * Clase interna para definir buckets de antigüedad
   */
  private record BucketDefinition(String key, String label, int minDays, int maxDays) {}
}
