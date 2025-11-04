package com.datakomerz.pymes.services;

import com.datakomerz.pymes.core.tenancy.CompanyContext;
import com.datakomerz.pymes.services.dto.ServiceReq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ServiceService {
    private final ServiceRepository serviceRepository;
    private final CompanyContext companyContext;

    public ServiceService(ServiceRepository serviceRepository, CompanyContext companyContext) {
        this.serviceRepository = serviceRepository;
        this.companyContext = companyContext;
    }

    @Transactional
    public com.datakomerz.pymes.services.Service create(ServiceReq req) {
        UUID companyId = companyContext.require();
        
        if (serviceRepository.existsByCompanyIdAndCode(companyId, req.code())) {
            throw new IllegalArgumentException("Ya existe un servicio con el código: " + req.code());
        }

        com.datakomerz.pymes.services.Service service = new com.datakomerz.pymes.services.Service();
        service.setCompanyId(companyId);
        service.setCode(req.code());
        service.setName(req.name());
        service.setDescription(req.description());
        service.setActive(req.active() != null ? req.active() : true);

        return serviceRepository.save(service);
    }

    @Transactional(readOnly = true)
    public List<com.datakomerz.pymes.services.Service> findAll() {
        UUID companyId = companyContext.require();
        return serviceRepository.findByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public List<com.datakomerz.pymes.services.Service> findByActive(Boolean active) {
        UUID companyId = companyContext.require();
        return serviceRepository.findByCompanyIdAndActive(companyId, active);
    }

    @Transactional(readOnly = true)
    public Page<com.datakomerz.pymes.services.Service> findAll(Pageable pageable) {
        UUID companyId = companyContext.require();
        return serviceRepository.findByCompanyId(companyId, pageable);
    }

    @Transactional(readOnly = true)
    public com.datakomerz.pymes.services.Service findById(UUID id) {
        UUID companyId = companyContext.require();
        return serviceRepository.findById(id)
            .filter(s -> s.getCompanyId().equals(companyId))
            .orElseThrow(() -> new IllegalArgumentException("Servicio no encontrado: " + id));
    }

    @Transactional
    public com.datakomerz.pymes.services.Service update(UUID id, ServiceReq req) {
        com.datakomerz.pymes.services.Service service = findById(id);
        
        if (!service.getCode().equals(req.code()) && 
            serviceRepository.existsByCompanyIdAndCode(service.getCompanyId(), req.code())) {
            throw new IllegalArgumentException("Ya existe un servicio con el código: " + req.code());
        }

        service.setCode(req.code());
        service.setName(req.name());
        service.setDescription(req.description());
        if (req.active() != null) {
            service.setActive(req.active());
        }

        return serviceRepository.save(service);
    }

    @Transactional
    public void delete(UUID id) {
        com.datakomerz.pymes.services.Service service = findById(id);
        serviceRepository.delete(service);
    }

    @Transactional(readOnly = true)
    public long countActive() {
        UUID companyId = companyContext.require();
        return serviceRepository.countByCompanyIdAndActive(companyId, true);
    }
}
