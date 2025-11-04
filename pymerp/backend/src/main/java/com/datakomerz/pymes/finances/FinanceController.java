package com.datakomerz.pymes.finances;

import com.datakomerz.pymes.finances.dto.AccountPayable;
import com.datakomerz.pymes.finances.dto.AccountReceivable;
import com.datakomerz.pymes.finances.dto.CashflowProjection;
import com.datakomerz.pymes.finances.dto.FinanceSummary;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/finances")
public class FinanceController {
  
  private final FinanceService financeService;
  
  public FinanceController(FinanceService financeService) {
    this.financeService = financeService;
  }
  
  /**
   * Obtiene el resumen financiero con KPIs principales
   * GET /api/v1/finances/summary
   */
  @GetMapping("/summary")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public ResponseEntity<FinanceSummary> getSummary() {
    FinanceSummary summary = financeService.getSummary();
    return ResponseEntity.ok(summary);
  }
  
  /**
   * Obtiene las cuentas por cobrar (ventas pendientes de pago)
   * GET /api/v1/finances/receivables?status=OVERDUE&page=0&size=20
   */
  @GetMapping("/receivables")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public ResponseEntity<Page<AccountReceivable>> getAccountsReceivable(
      @RequestParam(required = false) String status,
      @PageableDefault(size = 20) Pageable pageable
  ) {
    Page<AccountReceivable> receivables = financeService.getAccountsReceivable(status, pageable);
    return ResponseEntity.ok(receivables);
  }
  
  /**
   * Obtiene las cuentas por pagar (compras pendientes de pago)
   * GET /api/v1/finances/payables?status=DUE_SOON&page=0&size=20
   */
  @GetMapping("/payables")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public ResponseEntity<Page<AccountPayable>> getAccountsPayable(
      @RequestParam(required = false) String status,
      @PageableDefault(size = 20) Pageable pageable
  ) {
    Page<AccountPayable> payables = financeService.getAccountsPayable(status, pageable);
    return ResponseEntity.ok(payables);
  }
  
  /**
   * Obtiene la proyección de flujo de caja
   * GET /api/v1/finances/cashflow?days=30
   */
  @GetMapping("/cashflow")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public ResponseEntity<List<CashflowProjection>> getCashflowProjection(
      @RequestParam(defaultValue = "30") int days
  ) {
    if (days < 1 || days > 365) {
      return ResponseEntity.badRequest().build();
    }
    List<CashflowProjection> projections = financeService.getCashflowProjection(days);
    return ResponseEntity.ok(projections);
  }
}
