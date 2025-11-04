package com.datakomerz.pymes.services;

import com.datakomerz.pymes.services.dto.ServiceReq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/services")
public class ServiceController {
    private final ServiceService serviceService;

    public ServiceController(ServiceService serviceService) {
        this.serviceService = serviceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Service create(@RequestBody ServiceReq req) {
        return serviceService.create(req);
    }

    @GetMapping
    public Object findAll(@RequestParam(required = false) Boolean active,
                         @RequestParam(required = false) Integer page,
                         @RequestParam(required = false) Integer size) {
        if (page != null && size != null) {
            Pageable pageable = Pageable.ofSize(size).withPage(page);
            Page<Service> servicePage = serviceService.findAll(pageable);
            return servicePage;
        }
        
        if (active != null) {
            return serviceService.findByActive(active);
        }
        
        return serviceService.findAll();
    }

    @GetMapping("/{id}")
    public Service findById(@PathVariable UUID id) {
        return serviceService.findById(id);
    }

    @PutMapping("/{id}")
    public Service update(@PathVariable UUID id, @RequestBody ServiceReq req) {
        return serviceService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        serviceService.delete(id);
    }

    @GetMapping("/count")
    public long countActive() {
        return serviceService.countActive();
    }
}
