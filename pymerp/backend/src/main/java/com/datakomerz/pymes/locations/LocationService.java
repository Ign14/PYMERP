package com.datakomerz.pymes.locations;

import com.datakomerz.pymes.core.tenancy.CompanyContext;
import com.datakomerz.pymes.inventory.InventoryLot;
import com.datakomerz.pymes.inventory.InventoryLotRepository;
import com.datakomerz.pymes.locations.dto.LocationReq;
import com.datakomerz.pymes.locations.dto.LocationStockDTO;
import com.datakomerz.pymes.products.Product;
import com.datakomerz.pymes.products.ProductRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class LocationService {

    private final LocationRepository locationRepository;
    private final InventoryLotRepository inventoryLotRepository;
    private final ProductRepository productRepository;
    private final CompanyContext companyContext;

    public LocationService(
        LocationRepository locationRepository,
        InventoryLotRepository inventoryLotRepository,
        ProductRepository productRepository,
        CompanyContext companyContext
    ) {
        this.locationRepository = locationRepository;
        this.inventoryLotRepository = inventoryLotRepository;
        this.productRepository = productRepository;
        this.companyContext = companyContext;
    }

    @Transactional
    public Location create(LocationReq req) {
        UUID companyId = companyContext.require();
        String code = requireValue(req.code(), "codigo");

        if (locationRepository.existsByCompanyIdAndCode(companyId, code)) {
            throw new IllegalArgumentException("Ya existe una ubicación con el código: " + code);
        }

        Location location = new Location();
        location.setCompanyId(companyId);
        location.setCode(code);
        applyEditableFields(location, req);

        return locationRepository.save(location);
    }

    @Transactional(readOnly = true)
    public List<Location> findAll(LocationType type, LocationStatus status) {
        UUID companyId = companyContext.require();

        if (type != null && status != null) {
            return locationRepository.findByCompanyIdAndTypeAndStatus(companyId, type, status);
        }
        if (type != null) {
            return locationRepository.findByCompanyIdAndType(companyId, type);
        }
        if (status != null) {
            return locationRepository.findByCompanyIdAndStatus(companyId, status);
        }
        return locationRepository.findByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public Location findById(UUID id) {
        UUID companyId = companyContext.require();
        return locationRepository
            .findById(id)
            .filter(loc -> Objects.equals(loc.getCompanyId(), companyId))
            .orElseThrow(() -> new IllegalArgumentException("Ubicación no encontrada: " + id));
    }

    @Transactional
    public Location update(UUID id, LocationReq req) {
        Location location = findById(id);
        String code = requireValue(req.code(), "codigo");

        if (!location.getCode().equalsIgnoreCase(code) &&
            locationRepository.existsByCompanyIdAndCode(location.getCompanyId(), code)) {
            throw new IllegalArgumentException("Ya existe una ubicación con el código: " + code);
        }

        location.setCode(code);
        applyEditableFields(location, req);
        return locationRepository.save(location);
    }

    @Transactional
    public void delete(UUID id) {
        UUID companyId = companyContext.require();
        Location location = findById(id);
        
        // Validar que no existan lotes referenciando esta ubicación
        long referencedLots = inventoryLotRepository.countByCompanyIdAndLocationId(companyId, id);
        if (referencedLots > 0) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "No se puede eliminar la ubicación porque tiene " + referencedLots + " lote(s) asignado(s). Reasigne los lotes primero."
            );
        }
        
        locationRepository.delete(location);
    }

    @Transactional(readOnly = true)
    public List<LocationStockDTO> getLocationStockSummary() {
        UUID companyId = companyContext.require();
        List<Location> locations = locationRepository.findByCompanyId(companyId);

        return locations.stream()
            .map(location -> {
                List<InventoryLot> lots = inventoryLotRepository
                    .findByCompanyIdAndLocationId(companyId, location.getId());

                Map<UUID, List<InventoryLot>> lotsByProductId = lots.stream()
                    .collect(Collectors.groupingBy(InventoryLot::getProductId));

                Set<UUID> productIds = lotsByProductId.keySet();
                Map<UUID, Product> productsMap = productRepository
                    .findAllById(productIds)
                    .stream()
                    .collect(Collectors.toMap(Product::getId, p -> p));

                List<LocationStockDTO.ProductStock> productStocks = lotsByProductId.entrySet().stream()
                    .map(entry -> {
                        Product product = productsMap.get(entry.getKey());
                        if (product == null) {
                            return null;
                        }
                        BigDecimal totalQty = entry.getValue().stream()
                            .map(InventoryLot::getQtyAvailable)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                        return new LocationStockDTO.ProductStock(
                            product.getId(),
                            product.getName(),
                            product.getSku(),
                            totalQty,
                            entry.getValue().size()
                        );
                    })
                    .filter(Objects::nonNull)
                    .filter(stock -> stock.totalQuantity().compareTo(BigDecimal.ZERO) > 0)
                    .sorted(Comparator.comparing(LocationStockDTO.ProductStock::productName))
                    .toList();

                if (productStocks.isEmpty()) {
                    return null;
                }

                return new LocationStockDTO(
                    location.getId(),
                    location.getCode(),
                    location.getName(),
                    location.getType().name(),
                    productStocks
                );
            })
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(LocationStockDTO::locationName))
            .toList();
    }

    private void applyEditableFields(Location location, LocationReq req) {
        location.setName(requireValue(req.name(), "nombre"));
        location.setType(requireType(req.type()));
        location.setBusinessName(normalize(req.businessName()));
        location.setRut(normalizeRut(req.rut()));
        location.setDescription(normalize(req.description()));
        location.setStatus(req.status() != null ? req.status() : LocationStatus.ACTIVE);
    }

    private String requireValue(String value, String fieldName) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException("El " + fieldName + " es obligatorio");
        }
        return normalized;
    }

    private LocationType requireType(LocationType type) {
        if (type == null) {
            throw new IllegalArgumentException("El tipo de ubicación es obligatorio");
        }
        return type;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeRut(String value) {
        String normalized = normalize(value);
        return normalized != null ? normalized.toUpperCase() : null;
    }
}
