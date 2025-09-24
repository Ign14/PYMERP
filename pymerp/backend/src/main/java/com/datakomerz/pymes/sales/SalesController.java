package com.datakomerz.pymes.sales;

import com.datakomerz.pymes.sales.dto.SaleDetail;
import com.datakomerz.pymes.sales.dto.SaleReq;
import com.datakomerz.pymes.sales.dto.SaleRes;
import com.datakomerz.pymes.sales.dto.SaleSummary;
import com.datakomerz.pymes.sales.dto.SaleUpdateRequest;
import com.datakomerz.pymes.sales.dto.SalesDailyPoint;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sales")
public class SalesController {
  private final SalesService service;

  public SalesController(SalesService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SaleRes create(@Valid @RequestBody SaleReq req) {
    return service.create(req);
  }

  @PutMapping("/{id}")
  public SaleRes update(@PathVariable UUID id, @RequestBody SaleUpdateRequest req) {
    return service.update(id, req);
  }

  @PostMapping("/{id}/cancel")
  public SaleRes cancel(@PathVariable UUID id) {
    return service.cancel(id);
  }

  @GetMapping
  public Page<SaleSummary> list(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                @RequestParam(required = false) String status,
                                @RequestParam(required = false) String docType,
                                @RequestParam(required = false) String paymentMethod,
                                @RequestParam(required = false) String search,
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                OffsetDateTime from,
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                OffsetDateTime to) {
    return service.list(status, docType, paymentMethod, search, from, to, PageRequest.of(page, size));
  }

  @GetMapping("/{id}")
  public SaleDetail detail(@PathVariable UUID id) {
    return service.detail(id);
  }

  @GetMapping("/metrics/daily")
  public List<SalesDailyPoint> metrics(@RequestParam(defaultValue = "14") int days) {
    return service.dailyMetrics(days);
  }
}
