package com.datakomerz.pymes.locations;

import com.datakomerz.pymes.core.tenancy.CompanyContext;
import com.datakomerz.pymes.inventory.InventoryLot;
import com.datakomerz.pymes.inventory.InventoryLotRepository;
import com.datakomerz.pymes.locations.dto.LocationReq;
import com.datakomerz.pymes.locations.dto.LocationStockDTO;
import com.datakomerz.pymes.products.Product;
import com.datakomerz.pymes.products.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LocationService {
    private final LocationRepository locationRepository;
    private final InventoryLotRepository inventoryLotRepository;
    private final ProductRepository productRepository;
    private final CompanyContext companyContext;

    public LocationService(LocationRepository locationRepository, 
                          InventoryLotRepository inventoryLotRepository,
                          ProductRepository productRepository,
                          CompanyContext companyContext) {
        this.locationRepository = locationRepository;
        this.inventoryLotRepository = inventoryLotRepository;
        this.productRepository = productRepository;
        this.companyContext = companyContext;
    }

    @Transactional
    public Location create(LocationReq req) {
        UUID companyId = companyContext.require();
        
        // Validar que no exista otra ubicación con el mismo código
        if (locationRepository.existsByCompanyIdAndCode(companyId, req.code())) {
            throw new IllegalArgumentException("Ya existe una ubicación con el código: " + req.code());
        }

        Location location = new Location();
        location.setCompanyId(companyId);
        location.setCode(req.code());
        location.setName(req.name());
        location.setDescription(req.description());
        location.setType(req.type());
        location.setParentLocationId(req.parentLocationId());

        return locationRepository.save(location);
    }

    @Transactional(readOnly = true)
    public List<Location> findAll() {
        UUID companyId = companyContext.require();
        return locationRepository.findByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public List<Location> findByType(LocationType type) {
        UUID companyId = companyContext.require();
        return locationRepository.findByCompanyIdAndType(companyId, type);
    }

    @Transactional(readOnly = true)
    public List<Location> findByParentId(UUID parentLocationId) {
        UUID companyId = companyContext.require();
        return locationRepository.findByCompanyIdAndParentLocationId(companyId, parentLocationId);
    }

    @Transactional(readOnly = true)
    public Location findById(UUID id) {
        UUID companyId = companyContext.require();
        return locationRepository.findById(id)
            .filter(loc -> loc.getCompanyId().equals(companyId))
            .orElseThrow(() -> new IllegalArgumentException("Ubicación no encontrada: " + id));
    }

    @Transactional
    public Location update(UUID id, LocationReq req) {
        Location location = findById(id);
        
        // Si cambió el código, validar que no exista otro con el mismo
        if (!location.getCode().equals(req.code()) && 
            locationRepository.existsByCompanyIdAndCode(location.getCompanyId(), req.code())) {
            throw new IllegalArgumentException("Ya existe una ubicación con el código: " + req.code());
        }

        location.setCode(req.code());
        location.setName(req.name());
        location.setDescription(req.description());
        location.setType(req.type());
        location.setParentLocationId(req.parentLocationId());

        return locationRepository.save(location);
    }

    @Transactional
    public void delete(UUID id) {
        Location location = findById(id);
        locationRepository.delete(location);
    }

    @Transactional(readOnly = true)
    public List<LocationStockDTO> getLocationStockSummary() {
        UUID companyId = companyContext.require();
        List<Location> allLocations = locationRepository.findByCompanyId(companyId);
        
        // Agrupar por ubicación
        return allLocations.stream()
            .map(location -> {
                // Obtener todos los lotes de esta ubicación
                List<InventoryLot> lots = inventoryLotRepository.findByCompanyIdAndLocationId(companyId, location.getId());
                
                // Agrupar por productId y sumar cantidades
                Map<UUID, List<InventoryLot>> lotsByProductId = lots.stream()
                    .collect(Collectors.groupingBy(InventoryLot::getProductId));
                
                // Obtener todos los productos de los lotes
                Set<UUID> productIds = lotsByProductId.keySet();
                Map<UUID, Product> productsMap = productRepository.findAllById(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, p -> p));
                
                List<LocationStockDTO.ProductStock> productStocks = lotsByProductId.entrySet().stream()
                    .map(entry -> {
                        UUID productId = entry.getKey();
                        Product product = productsMap.get(productId);
                        if (product == null) {
                            return null; // Skip products not found
                        }
                        
                        List<InventoryLot> productLots = entry.getValue();
                        
                        BigDecimal totalQty = productLots.stream()
                            .map(InventoryLot::getQtyAvailable)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                        
                        return new LocationStockDTO.ProductStock(
                            product.getId(),
                            product.getName(),
                            product.getSku(),
                            totalQty,
                            productLots.size()
                        );
                    })
                    .filter(Objects::nonNull)
                    .filter(ps -> ps.totalQuantity().compareTo(BigDecimal.ZERO) > 0) // Solo productos con stock
                    .sorted(Comparator.comparing(LocationStockDTO.ProductStock::productName))
                    .toList();
                
                return new LocationStockDTO(
                    location.getId(),
                    location.getCode(),
                    location.getName(),
                    location.getType().name(),
                    productStocks
                );
            })
            .filter(dto -> !dto.products().isEmpty()) // Solo ubicaciones con stock
            .sorted(Comparator.comparing(LocationStockDTO::locationName))
            .toList();
    }
}
