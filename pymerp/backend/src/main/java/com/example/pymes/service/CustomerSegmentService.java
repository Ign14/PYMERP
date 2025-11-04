package com.example.pymes.service;

import com.datakomerz.pymes.core.tenancy.CompanyContext;
import com.example.pymes.dto.CustomerSegmentRequest;
import com.example.pymes.dto.CustomerSegmentStatsResponse;
import com.example.pymes.entity.CustomerSegment;
import com.example.pymes.repository.CustomerSegmentRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing customer segments.
 */
@Service
public class CustomerSegmentService {

    private final CustomerSegmentRepository repository;
    private final CompanyContext companyContext;

    public CustomerSegmentService(CustomerSegmentRepository repository, CompanyContext companyContext) {
        this.repository = repository;
        this.companyContext = companyContext;
    }

    /**
     * Find all segments for current company.
     */
    @Transactional(readOnly = true)
    public List<CustomerSegment> findAll() {
        UUID companyId = companyContext.require();
        return repository.findByCompanyIdOrderByNameAsc(companyId);
    }

    /**
     * Find all active segments for current company.
     */
    @Transactional(readOnly = true)
    public List<CustomerSegment> findActive() {
        UUID companyId = companyContext.require();
        return repository.findByCompanyIdAndActiveOrderByNameAsc(companyId, true);
    }

    /**
     * Find segment by ID.
     */
    @Transactional(readOnly = true)
    public CustomerSegment findById(UUID id) {
        UUID companyId = companyContext.require();
        CustomerSegment segment = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Segment not found: " + id));
        if (!segment.getCompanyId().equals(companyId)) {
            throw new IllegalArgumentException("Segment does not belong to company");
        }
        return segment;
    }

    /**
     * Find segment by code.
     */
    @Transactional(readOnly = true)
    public CustomerSegment findByCode(String code) {
        UUID companyId = companyContext.require();
        return repository.findByCompanyIdAndCode(companyId, code.toUpperCase())
            .orElseThrow(() -> new IllegalArgumentException("Segment not found: " + code));
    }

    /**
     * Create new segment.
     */
    @Transactional
    public CustomerSegment create(CustomerSegmentRequest request) {
        UUID companyId = companyContext.require();
        String code = request.getCode().trim().toUpperCase();
        
        if (repository.existsByCompanyIdAndCode(companyId, code)) {
            throw new IllegalArgumentException("Segment code already exists: " + code);
        }

        CustomerSegment segment = new CustomerSegment();
        segment.setCompanyId(companyId);
        segment.setCode(code);
        segment.setName(request.getName().trim());
        segment.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        segment.setColor(request.getColor() != null ? request.getColor().trim() : null);
        segment.setActive(request.getActive() != null ? request.getActive() : true);

        return repository.save(segment);
    }

    /**
     * Update existing segment.
     */
    @Transactional
    public CustomerSegment update(UUID id, CustomerSegmentRequest request) {
        CustomerSegment segment = findById(id);
        
        // Check if code is being changed and if new code already exists
        String newCode = request.getCode().trim().toUpperCase();
        if (!segment.getCode().equals(newCode)) {
            if (repository.existsByCompanyIdAndCode(segment.getCompanyId(), newCode)) {
                throw new IllegalArgumentException("Segment code already exists: " + newCode);
            }
            segment.setCode(newCode);
        }

        segment.setName(request.getName().trim());
        segment.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        segment.setColor(request.getColor() != null ? request.getColor().trim() : null);
        if (request.getActive() != null) {
            segment.setActive(request.getActive());
        }

        return repository.save(segment);
    }

    /**
     * Delete segment (soft delete by setting active = false).
     */
    @Transactional
    public void delete(UUID id) {
        CustomerSegment segment = findById(id);
        segment.setActive(false);
        repository.save(segment);
    }

    /**
     * Get segment statistics with customer counts.
     */
    @Transactional(readOnly = true)
    public List<CustomerSegmentStatsResponse> getSegmentStats() {
        UUID companyId = companyContext.require();
        List<Object[]> results = repository.findSegmentStatsForCompany(companyId);
        
        List<CustomerSegmentStatsResponse> stats = new ArrayList<>();
        for (Object[] row : results) {
            String code = (String) row[0];
            String name = (String) row[1];
            String color = (String) row[2];
            Long total = (Long) row[3];
            stats.add(new CustomerSegmentStatsResponse(code, name, color, total));
        }
        
        return stats;
    }
}
