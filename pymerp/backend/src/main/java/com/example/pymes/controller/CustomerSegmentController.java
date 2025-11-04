package com.example.pymes.controller;

import com.example.pymes.dto.CustomerSegmentRequest;
import com.example.pymes.dto.CustomerSegmentResponse;
import com.example.pymes.dto.CustomerSegmentStatsResponse;
import com.example.pymes.entity.CustomerSegment;
import com.example.pymes.mapper.CustomerSegmentMapper;
import com.example.pymes.service.CustomerSegmentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for customer segment management.
 */
@RestController
@RequestMapping("/api/v1/customer-segments")
public class CustomerSegmentController {

    private final CustomerSegmentService service;
    private final CustomerSegmentMapper mapper;

    public CustomerSegmentController(CustomerSegmentService service, CustomerSegmentMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    /**
     * List all segments for current company.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
    public ResponseEntity<List<CustomerSegmentResponse>> list() {
        List<CustomerSegment> segments = service.findAll();
        List<CustomerSegmentResponse> response = segments.stream()
            .map(mapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Get segment statistics with customer counts.
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
    public ResponseEntity<List<CustomerSegmentStatsResponse>> stats() {
        List<CustomerSegmentStatsResponse> stats = service.getSegmentStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get segment by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
    public ResponseEntity<CustomerSegmentResponse> getById(@PathVariable UUID id) {
        CustomerSegment segment = service.findById(id);
        return ResponseEntity.ok(mapper.toResponse(segment));
    }

    /**
     * Create new segment.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
    public ResponseEntity<CustomerSegmentResponse> create(@Valid @RequestBody CustomerSegmentRequest request) {
        CustomerSegment segment = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(segment));
    }

    /**
     * Update existing segment.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
    public ResponseEntity<CustomerSegmentResponse> update(
        @PathVariable UUID id,
        @Valid @RequestBody CustomerSegmentRequest request
    ) {
        CustomerSegment segment = service.update(id, request);
        return ResponseEntity.ok(mapper.toResponse(segment));
    }

    /**
     * Delete segment (soft delete).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
