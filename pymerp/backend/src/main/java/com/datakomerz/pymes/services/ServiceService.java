package com.datakomerz.pymes.services;

import com.datakomerz.pymes.core.tenancy.CompanyContext;
import com.datakomerz.pymes.services.dto.ServiceReq;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final CompanyContext companyContext;

    public ServiceService(ServiceRepository serviceRepository, CompanyContext companyContext) {
        this.serviceRepository = serviceRepository;
        this.companyContext = companyContext;
    }

    @Transactional
    public Service create(ServiceReq req) {
        UUID companyId = companyContext.require();
        String code = requireValue(req.code(), "código");

        if (serviceRepository.existsByCompanyIdAndCode(companyId, code)) {
            throw new IllegalArgumentException("Ya existe un servicio con el código: " + code);
        }

        Service service = new Service();
        service.setCompanyId(companyId);
        service.setCode(code);
        applyEditableFields(service, req);
        return serviceRepository.save(service);
    }

    @Transactional(readOnly = true)
    public List<Service> findAll(ServiceStatus status) {
        UUID companyId = companyContext.require();
        if (status != null) {
            return serviceRepository.findByCompanyIdAndStatus(companyId, status);
        }
        return serviceRepository.findByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public Page<Service> findAll(ServiceStatus status, Pageable pageable) {
        UUID companyId = companyContext.require();
        if (status != null) {
            return serviceRepository.findByCompanyIdAndStatus(companyId, status, pageable);
        }
        return serviceRepository.findByCompanyId(companyId, pageable);
    }

    @Transactional(readOnly = true)
    public Service findById(UUID id) {
        UUID companyId = companyContext.require();
        return serviceRepository.findById(id)
            .filter(service -> Objects.equals(service.getCompanyId(), companyId))
            .orElseThrow(() -> new IllegalArgumentException("Servicio no encontrado: " + id));
    }

    @Transactional
    public Service update(UUID id, ServiceReq req) {
        Service service = findById(id);
        String code = requireValue(req.code(), "código");

        if (!service.getCode().equalsIgnoreCase(code) &&
            serviceRepository.existsByCompanyIdAndCode(service.getCompanyId(), code)) {
            throw new IllegalArgumentException("Ya existe un servicio con el código: " + code);
        }

        service.setCode(code);
        applyEditableFields(service, req);
        return serviceRepository.save(service);
    }

    @Transactional
    public void delete(UUID id) {
        Service service = findById(id);
        serviceRepository.delete(service);
    }

    @Transactional(readOnly = true)
    public long countActive() {
        UUID companyId = companyContext.require();
        return serviceRepository.countByCompanyIdAndStatus(companyId, ServiceStatus.ACTIVE);
    }

    private void applyEditableFields(Service service, ServiceReq req) {
        service.setName(requireValue(req.name(), "nombre"));
        service.setDescription(normalize(req.description()));
        service.setCategory(normalize(req.category()));
        service.setUnitPrice(requirePositivePrice(req.unitPrice()));
        service.setStatus(req.status() != null ? req.status() : ServiceStatus.ACTIVE);
    }

    private String requireValue(String value, String field) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException("El " + field + " es obligatorio");
        }
        return normalized;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BigDecimal requirePositivePrice(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("El precio unitario es obligatorio");
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio unitario debe ser mayor a cero");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
