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

        // Validar jerarquía circular si tiene padre
        if (req.parentLocationId() != null) {
            validateNoCircularHierarchy(null, req.parentLocationId());
        }

        Location location = new Location();
        location.setCompanyId(companyId);
        location.setCode(req.code());
        location.setName(req.name());
        location.setDescription(req.description());
        location.setType(req.type());
        location.setParentLocationId(req.parentLocationId());
        location.setActive(req.active() != null ? req.active() : true);
        location.setIsBlocked(req.isBlocked() != null ? req.isBlocked() : false);
        location.setCapacity(req.capacity());
        location.setCapacityUnit(req.capacityUnit() != null ? req.capacityUnit() : "UNITS");

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

        // Validar jerarquía circular si cambió el padre
        if (req.parentLocationId() != null && !Objects.equals(location.getParentLocationId(), req.parentLocationId())) {
            validateNoCircularHierarchy(id, req.parentLocationId());
        }

        location.setCode(req.code());
        location.setName(req.name());
        location.setDescription(req.description());
        location.setType(req.type());
        location.setParentLocationId(req.parentLocationId());
        
        if (req.active() != null) {
            location.setActive(req.active());
        }
        if (req.isBlocked() != null) {
            location.setIsBlocked(req.isBlocked());
        }
        if (req.capacity() != null) {
            location.setCapacity(req.capacity());
        }
        if (req.capacityUnit() != null) {
            location.setCapacityUnit(req.capacityUnit());
        }

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

    /**
     * Valida que no se cree una jerarquía circular
     * @param locationId ID de la ubicación que se está actualizando (null si es creación)
     * @param newParentId ID del nuevo padre propuesto
     */
    private void validateNoCircularHierarchy(UUID locationId, UUID newParentId) {
        if (newParentId == null) {
            return;
        }

        // Una ubicación no puede ser su propio padre
        if (Objects.equals(locationId, newParentId)) {
            throw new IllegalArgumentException("Una ubicación no puede ser su propio padre");
        }

        // Verificar que el nuevo padre no sea un descendiente de la ubicación actual
        if (locationId != null) {
            Set<UUID> descendants = getAllDescendantIds(locationId);
            if (descendants.contains(newParentId)) {
                throw new IllegalArgumentException("No se puede crear jerarquía circular: el nuevo padre es un descendiente de esta ubicación");
            }
        }

        // Verificar que no se exceda un límite razonable de profundidad (ej: 10 niveles)
        int depth = getHierarchyDepth(newParentId);
        if (depth >= 10) {
            throw new IllegalArgumentException("Profundidad máxima de jerarquía alcanzada (10 niveles)");
        }
    }

    /**
     * Obtiene la profundidad de la jerarquía desde una ubicación hacia arriba
     */
    private int getHierarchyDepth(UUID locationId) {
        int depth = 0;
        UUID currentId = locationId;
        Set<UUID> visited = new HashSet<>();

        while (currentId != null && depth < 20) { // Límite de seguridad
            if (visited.contains(currentId)) {
                // Jerarquía circular detectada
                throw new IllegalStateException("Jerarquía circular detectada en ubicaciones");
            }
            visited.add(currentId);

            Optional<Location> current = locationRepository.findById(currentId);
            if (current.isEmpty()) {
                break;
            }
            currentId = current.get().getParentLocationId();
            depth++;
        }

        return depth;
    }

    /**
     * Obtiene todos los IDs de descendientes de una ubicación
     */
    private Set<UUID> getAllDescendantIds(UUID locationId) {
        Set<UUID> descendants = new HashSet<>();
        Queue<UUID> queue = new LinkedList<>();
        queue.add(locationId);

        while (!queue.isEmpty()) {
            UUID current = queue.poll();
            List<Location> children = locationRepository.findByParentLocationId(current);
            
            for (Location child : children) {
                if (!descendants.contains(child.getId())) {
                    descendants.add(child.getId());
                    queue.add(child.getId());
                }
            }
        }

        return descendants;
    }

    /**
     * Obtiene el path completo de una ubicación (desde raíz hasta la ubicación)
     * @return Lista de ubicaciones desde la raíz hasta la ubicación actual
     */
    @Transactional(readOnly = true)
    public List<Location> getLocationPath(UUID locationId) {
        List<Location> path = new ArrayList<>();
        UUID currentId = locationId;
        Set<UUID> visited = new HashSet<>();

        while (currentId != null) {
            if (visited.contains(currentId)) {
                throw new IllegalStateException("Jerarquía circular detectada");
            }
            visited.add(currentId);

            Optional<Location> current = locationRepository.findById(currentId);
            if (current.isEmpty()) {
                break;
            }
            
            Location loc = current.get();
            path.add(0, loc); // Insertar al inicio para tener orden raíz -> hoja
            currentId = loc.getParentLocationId();
        }

        return path;
    }

    /**
     * Obtiene todos los hijos directos de una ubicación
     */
    @Transactional(readOnly = true)
    public List<Location> getChildren(UUID parentId) {
        UUID companyId = companyContext.require();
        return locationRepository.findByCompanyIdAndParentLocationId(companyId, parentId);
    }

    /**
     * Obtiene todos los descendientes (hijos, nietos, etc.) de una ubicación
     */
    @Transactional(readOnly = true)
    public List<Location> getAllDescendants(UUID locationId) {
        List<Location> descendants = new ArrayList<>();
        Set<UUID> descendantIds = getAllDescendantIds(locationId);
        
        if (!descendantIds.isEmpty()) {
            descendants = locationRepository.findAllById(descendantIds);
        }
        
        return descendants;
    }

    /**
     * Calcula la capacidad utilizada actual de una ubicación
     */
    @Transactional(readOnly = true)
    public BigDecimal getCurrentCapacityUsed(UUID locationId) {
        UUID companyId = companyContext.require();
        List<InventoryLot> lots = inventoryLotRepository.findByCompanyIdAndLocationId(companyId, locationId);
        
        return lots.stream()
            .map(InventoryLot::getQtyAvailable)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Verifica si una ubicación puede recibir stock (activa, no bloqueada, con capacidad)
     */
    @Transactional(readOnly = true)
    public boolean canReceiveStock(UUID locationId, BigDecimal quantity) {
        Location location = findById(locationId);
        
        // Verificar estado
        if (!location.getActive() || location.getIsBlocked()) {
            return false;
        }

        // Si tiene capacidad definida, verificar que no se exceda
        if (location.getCapacity() != null && location.getCapacity().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentUsed = getCurrentCapacityUsed(locationId);
            BigDecimal afterAddition = currentUsed.add(quantity);
            
            return afterAddition.compareTo(location.getCapacity()) <= 0;
        }

        // Sin límite de capacidad
        return true;
    }

    /**
     * Obtiene todas las ubicaciones activas
     */
    @Transactional(readOnly = true)
    public List<Location> findAllActive() {
        UUID companyId = companyContext.require();
        return locationRepository.findByCompanyId(companyId).stream()
            .filter(Location::getActive)
            .filter(loc -> !loc.getIsBlocked())
            .toList();
    }
}
