package com.datakomerz.pymes.services;

import com.datakomerz.pymes.services.dto.ServiceReq;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/services")
public class ServiceController {

    private final ServiceService serviceService;

    public ServiceController(ServiceService serviceService) {
        this.serviceService = serviceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
    public Service create(@RequestBody ServiceReq req) {
        return serviceService.create(req);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
    public Object findAll(
        @RequestParam(required = false) ServiceStatus status,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        if (page != null && size != null) {
            Pageable pageable = Pageable.ofSize(size).withPage(page);
            return serviceService.findAll(status, pageable);
        }
        return serviceService.findAll(status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
    public Service findById(@PathVariable UUID id) {
        return serviceService.findById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
    public Service update(@PathVariable UUID id, @RequestBody ServiceReq req) {
        return serviceService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable UUID id) {
        serviceService.delete(id);
    }

    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
    public long countActive() {
        return serviceService.countActive();
    }
}
