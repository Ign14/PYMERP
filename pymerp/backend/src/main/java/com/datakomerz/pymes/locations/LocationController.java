package com.datakomerz.pymes.locations;

import com.datakomerz.pymes.locations.dto.LocationReq;
import com.datakomerz.pymes.locations.dto.LocationStockDTO;
import com.datakomerz.pymes.locations.dto.LocationWithHierarchy;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/locations")
public class LocationController {
    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
    public Location create(@RequestBody LocationReq req) {
        return locationService.create(req);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
    public List<Location> findAll(@RequestParam(required = false) String type) {
        if (type != null) {
            return locationService.findByType(LocationType.valueOf(type));
        }
        return locationService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
    public Location findById(@PathVariable UUID id) {
        return locationService.findById(id);
    }

    @GetMapping("/children/{parentId}")
    @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
    public List<Location> findByParentId(@PathVariable UUID parentId) {
        return locationService.findByParentId(parentId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
    public Location update(@PathVariable UUID id, @RequestBody LocationReq req) {
        return locationService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable UUID id) {
        locationService.delete(id);
    }

    @GetMapping("/stock-summary")
    @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
    public List<LocationStockDTO> getLocationStockSummary() {
        return locationService.getLocationStockSummary();
    }

    @GetMapping("/{id}/path")
    @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
    public List<Location> getLocationPath(@PathVariable UUID id) {
        return locationService.getLocationPath(id);
    }

    @GetMapping("/{id}/children")
    @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
    public List<Location> getChildren(@PathVariable UUID id) {
        return locationService.getChildren(id);
    }

    @GetMapping("/{id}/descendants")
    @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
    public List<Location> getAllDescendants(@PathVariable UUID id) {
        return locationService.getAllDescendants(id);
    }

    @GetMapping("/{id}/capacity")
    @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
    public Map<String, Object> getCapacityInfo(@PathVariable UUID id) {
        Location location = locationService.findById(id);
        BigDecimal currentUsed = locationService.getCurrentCapacityUsed(id);
        
        BigDecimal available = null;
        Double percentUsed = null;
        
        if (location.getCapacity() != null && location.getCapacity().compareTo(BigDecimal.ZERO) > 0) {
            available = location.getCapacity().subtract(currentUsed);
            percentUsed = currentUsed.divide(location.getCapacity(), 4, BigDecimal.ROUND_HALF_UP)
                                    .multiply(BigDecimal.valueOf(100))
                                    .doubleValue();
        }
        
        return Map.of(
            "locationId", location.getId(),
            "capacity", location.getCapacity() != null ? location.getCapacity() : 0,
            "capacityUnit", location.getCapacityUnit(),
            "currentUsed", currentUsed,
            "available", available != null ? available : 0,
            "percentUsed", percentUsed != null ? percentUsed : 0,
            "canReceiveMore", location.getCapacity() == null || available.compareTo(BigDecimal.ZERO) > 0
        );
    }

    @GetMapping("/{id}/can-receive")
    @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
    public Map<String, Boolean> canReceiveStock(@PathVariable UUID id, @RequestParam BigDecimal quantity) {
        boolean canReceive = locationService.canReceiveStock(id, quantity);
        return Map.of("canReceive", canReceive);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
    public List<Location> findAllActive() {
        return locationService.findAllActive();
    }
}
