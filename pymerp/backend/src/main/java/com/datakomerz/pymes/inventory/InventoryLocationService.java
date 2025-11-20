package com.datakomerz.pymes.inventory;

import com.datakomerz.pymes.core.tenancy.CompanyContext;
import com.datakomerz.pymes.inventory.dto.InventoryLocationPatchRequest;
import com.datakomerz.pymes.inventory.dto.InventoryLocationRequest;
import com.datakomerz.pymes.inventory.dto.InventoryLocationResponse;
import com.datakomerz.pymes.multitenancy.ValidateTenant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InventoryLocationService {

  private final InventoryLocationRepository locations;
  private final InventoryLotRepository inventoryLotRepository;
  private final CompanyContext companyContext;

  public InventoryLocationService(
      InventoryLocationRepository locations,
      InventoryLotRepository inventoryLotRepository,
      CompanyContext companyContext) {
    this.locations = locations;
    this.inventoryLotRepository = inventoryLotRepository;
    this.companyContext = companyContext;
  }

  public Page<InventoryLocationResponse> search(String query, Boolean enabled, Pageable pageable) {
    UUID companyId = companyContext.require();
    String normalizedQuery = normalizeQuery(query);
    Boolean effectiveEnabled = enabled == null ? Boolean.TRUE : enabled;
    Page<InventoryLocation> page = locations.search(companyId, effectiveEnabled, normalizedQuery, pageable);
    return page.map(this::toResponse);
  }

  @Transactional
  public InventoryLocationResponse create(InventoryLocationRequest request) {
    UUID companyId = companyContext.require();
    String code = normalizeText(request.code());
    String name = requireName(request.name());
    ensureUnique(companyId, name, code, null);

    InventoryLocation location = new InventoryLocation();
    location.setCompanyId(companyId);
    location.setCode(code);
    location.setName(name);
    location.setDescription(normalizeText(request.description()));
    location.setEnabled(request.enabled() == null ? true : request.enabled());
    InventoryLocation saved = locations.save(location);
    return toResponse(saved);
  }

  @Transactional
  @ValidateTenant(entityClass = InventoryLocation.class)
  public InventoryLocationResponse update(UUID id, InventoryLocationRequest request) {
    UUID companyId = companyContext.require();
    InventoryLocation location = locations.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Ubicación no encontrada: " + id));
    String code = normalizeText(request.code());
    String name = requireName(request.name());
    ensureUnique(companyId, name, code, id);
    location.setCode(code);
    location.setName(name);
    location.setDescription(normalizeText(request.description()));
    location.setEnabled(request.enabled() == null ? true : request.enabled());
    InventoryLocation saved = locations.save(location);
    return toResponse(saved);
  }

  @Transactional
  @ValidateTenant(entityClass = InventoryLocation.class)
  public InventoryLocationResponse patch(UUID id, InventoryLocationPatchRequest request) {
    UUID companyId = companyContext.require();
    InventoryLocation location = locations.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Ubicación no encontrada: " + id));
    boolean changed = false;
    String newCode = request.code() != null ? normalizeText(request.code()) : location.getCode();
    if (!StringUtils.hasText(newCode) && location.getCode() != null) {
      // allow removal by setting explicitly to blank
      newCode = null;
    }
    String newName = request.name() != null ? normalizeText(request.name()) : location.getName();
    if (request.name() != null && !StringUtils.hasText(newName)) {
      throw new IllegalArgumentException("El nombre no puede estar vacío");
    }
    if (!newName.equals(location.getName())) {
      changed = true;
    }
    if (!((newCode == null && location.getCode() == null) ||
          (newCode != null && newCode.equals(location.getCode())))) {
      changed = true;
    }
    if (changed) {
      ensureUnique(companyId, newName, newCode, location.getId());
    }
    if (request.code() != null) {
      location.setCode(newCode);
    }
    if (request.name() != null) {
      location.setName(newName);
    }
    if (request.description() != null) {
      location.setDescription(normalizeText(request.description()));
    }
    if (request.enabled() != null) {
      location.setEnabled(request.enabled());
    }
    InventoryLocation saved = locations.save(location);
    return toResponse(saved);
  }

  @Transactional
  @ValidateTenant(entityClass = InventoryLocation.class)
  public void softDelete(UUID id) {
    UUID companyId = companyContext.require();
    InventoryLocation location = locations.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Ubicación no encontrada: " + id));
    long assignedLots = inventoryLotRepository.countByCompanyIdAndLocationId(companyId, id);
    if (assignedLots > 0) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "No se puede eliminar la ubicación porque tiene " + assignedLots + " lote(s) asignado(s). Reasigne los lotes primero.");
    }
    location.setEnabled(false);
    locations.save(location);
  }

  InventoryLocationResponse toResponse(InventoryLocation location) {
    return new InventoryLocationResponse(
        location.getId(),
        location.getCode(),
        location.getName(),
        location.getDescription(),
        Boolean.TRUE.equals(location.getEnabled()),
        location.getCreatedAt(),
        location.getUpdatedAt());
  }

  private String normalizeText(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private String normalizeQuery(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private String requireName(String name) {
    String normalized = normalizeText(name);
    if (!StringUtils.hasText(normalized)) {
      throw new IllegalArgumentException("El nombre es obligatorio");
    }
    return normalized;
  }

  private void ensureUnique(UUID companyId, String name, String code, UUID currentId) {
    if (name != null) {
      boolean exists;
      if (currentId == null) {
        exists = locations.existsByCompanyIdAndName(companyId, name);
      } else {
        exists = locations.existsByCompanyIdAndNameAndIdNot(companyId, name, currentId);
      }
      if (exists) {
        throw new IllegalArgumentException("Ya existe una ubicación con ese nombre");
      }
    }
    if (code != null) {
      boolean exists;
      if (currentId == null) {
        exists = locations.existsByCompanyIdAndCode(companyId, code);
      } else {
        exists = locations.existsByCompanyIdAndCodeAndIdNot(companyId, code, currentId);
      }
      if (exists) {
        throw new IllegalArgumentException("Ya existe una ubicación con ese código");
      }
    }
  }
}
