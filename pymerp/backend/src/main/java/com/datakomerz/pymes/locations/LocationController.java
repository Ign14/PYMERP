package com.datakomerz.pymes.locations;

import com.datakomerz.pymes.locations.dto.LocationReq;
import com.datakomerz.pymes.locations.dto.LocationStockDTO;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
}
