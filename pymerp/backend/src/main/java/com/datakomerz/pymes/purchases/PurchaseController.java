package com.datakomerz.pymes.purchases;

import com.datakomerz.pymes.purchases.dto.PurchaseDailyPoint;
import com.datakomerz.pymes.purchases.dto.PurchaseReq;
import com.datakomerz.pymes.purchases.dto.PurchaseSummary;
import com.datakomerz.pymes.purchases.dto.PurchaseUpdateRequest;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/api/v1/purchases")
public class PurchaseController {
  private final PurchaseService service;
  public PurchaseController(PurchaseService service) { this.service = service; }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String,Object> create(@Valid @RequestBody PurchaseReq req) {
    var id = service.create(req);
    return Map.of("id", id);
  }

  @PutMapping("/{id}")
  public PurchaseSummary update(@PathVariable UUID id, @RequestBody PurchaseUpdateRequest req) {
    return service.update(id, req);
  }

  @PostMapping("/{id}/cancel")
  public PurchaseSummary cancel(@PathVariable UUID id) {
    return service.cancel(id);
  }

  @GetMapping
  public Page<PurchaseSummary> list(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) String docType,
                                    @RequestParam(required = false) String search,
                                    @RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                    OffsetDateTime from,
                                    @RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                    OffsetDateTime to) {
    return service.list(status, docType, search, from, to, PageRequest.of(page, size));
  }

  @GetMapping("/metrics/daily")
  public List<PurchaseDailyPoint> metrics(@RequestParam(defaultValue = "14") int days) {
    return service.dailyMetrics(days);
  }
}
