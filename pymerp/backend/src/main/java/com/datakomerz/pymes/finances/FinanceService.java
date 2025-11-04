package com.datakomerz.pymes.finances;

import com.datakomerz.pymes.core.tenancy.CompanyContext;
import com.datakomerz.pymes.customers.CustomerRepository;
import com.datakomerz.pymes.finances.dto.AccountPayable;
import com.datakomerz.pymes.finances.dto.AccountReceivable;
import com.datakomerz.pymes.finances.dto.CashflowProjection;
import com.datakomerz.pymes.finances.dto.FinanceSummary;
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
  private final CompanyContext companyContext;

  public FinanceService(
      SaleRepository saleRepository,
      PurchaseRepository purchaseRepository,
      CustomerRepository customerRepository,
      SupplierRepository supplierRepository,
      CompanyContext companyContext) {
    this.saleRepository = saleRepository;
    this.purchaseRepository = purchaseRepository;
    this.customerRepository = customerRepository;
    this.supplierRepository = supplierRepository;
    this.companyContext = companyContext;
  }

  @Transactional(readOnly = true)
  public FinanceSummary getSummary() {
    UUID companyId = companyContext.require();
    
    // Obtener todas las ventas pendientes de pago (excluyendo canceladas)
    List<Sale> pendingSales = saleRepository.findByCompanyIdOrderByIssuedAtDesc(companyId, Pageable.unpaged())
        .getContent()
        .stream()
        .filter(s -> s.getStatus() != null && !"cancelled".equalsIgnoreCase(s.getStatus()))
        .toList();
    
    // Obtener todas las compras pendientes de pago (excluyendo canceladas)
    List<Purchase> pendingPurchases = purchaseRepository.findByCompanyIdOrderByIssuedAtDesc(companyId, Pageable.unpaged())
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
        .filter(s -> s.getIssuedAt() != null && s.getIssuedAt().plusDays(30).isBefore(now))
        .count();
    
    BigDecimal next7DaysReceivable = pendingSales.stream()
        .filter(s -> s.getIssuedAt() != null && s.getIssuedAt().plusDays(30).isBefore(next7Days))
        .map(s -> s.getTotal() != null ? s.getTotal() : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal next30DaysReceivable = pendingSales.stream()
        .filter(s -> s.getIssuedAt() != null && s.getIssuedAt().plusDays(30).isBefore(next30Days))
        .map(s -> s.getTotal() != null ? s.getTotal() : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    // Calcular cuentas por pagar
    BigDecimal totalPayable = pendingPurchases.stream()
        .map(p -> p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    long overduePurchases = pendingPurchases.stream()
        .filter(p -> p.getIssuedAt() != null && p.getIssuedAt().plusDays(30).isBefore(now))
        .count();
    
    BigDecimal next7DaysPayable = pendingPurchases.stream()
        .filter(p -> p.getIssuedAt() != null && p.getIssuedAt().plusDays(30).isBefore(next7Days))
        .map(p -> p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal next30DaysPayable = pendingPurchases.stream()
        .filter(p -> p.getIssuedAt() != null && p.getIssuedAt().plusDays(30).isBefore(next30Days))
        .map(p -> p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    // Posición neta (sin caja física por ahora)
    BigDecimal netPosition = totalReceivable.subtract(totalPayable);
    
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
        next30DaysPayable
    );
  }

  @Transactional(readOnly = true)
  public Page<AccountReceivable> getAccountsReceivable(String status, Pageable pageable) {
    UUID companyId = companyContext.require();
    
    List<Sale> sales = saleRepository.findByCompanyIdOrderByIssuedAtDesc(companyId, Pageable.unpaged())
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
              paymentStatus
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
    UUID companyId = companyContext.require();
    
    List<Purchase> purchases = purchaseRepository.findByCompanyIdOrderByIssuedAtDesc(companyId, Pageable.unpaged())
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
              paymentStatus
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
    UUID companyId = companyContext.require();
    
    List<Sale> sales = saleRepository.findByCompanyIdOrderByIssuedAtDesc(companyId, Pageable.unpaged())
        .getContent()
        .stream()
        .filter(s -> s.getStatus() != null && !"cancelled".equalsIgnoreCase(s.getStatus()))
        .toList();
    
    List<Purchase> purchases = purchaseRepository.findByCompanyIdOrderByIssuedAtDesc(companyId, Pageable.unpaged())
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
}
