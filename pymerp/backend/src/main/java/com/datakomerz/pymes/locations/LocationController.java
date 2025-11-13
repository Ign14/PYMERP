package com.datakomerz.pymes.locations;

import com.datakomerz.pymes.locations.dto.LocationReq;
import com.datakomerz.pymes.locations.dto.LocationStockDTO;
import java.util.List;
import java.util.UUID;
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
@RequestMapping("/api/v1/inventory/locations")
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
    public List<Location> findAll(
        @RequestParam(required = false) LocationType type,
        @RequestParam(required = false) LocationStatus status
    ) {
        return locationService.findAll(type, status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
    public Location findById(@PathVariable UUID id) {
        return locationService.findById(id);
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
