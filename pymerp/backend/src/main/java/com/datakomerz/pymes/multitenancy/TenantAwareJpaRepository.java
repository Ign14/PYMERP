package com.datakomerz.pymes.multitenancy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.lang.NonNull;

/**
 * Repository base class that enforces tenant isolation for entities annotated
 * with {@link TenantFiltered}. It supplements Hibernate filters by explicitly
 * checking the tenant identifier on entity-based operations such as
 * {@code findById}, {@code getReferenceById} and deletions.
 */
@NoRepositoryBean
public class TenantAwareJpaRepository<T, ID> extends SimpleJpaRepository<T, ID> {

  private static final Logger logger = LoggerFactory.getLogger(TenantAwareJpaRepository.class);

  private final JpaEntityInformation<T, ?> entityInformation;

  public TenantAwareJpaRepository(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
    super(entityInformation, entityManager);
    this.entityInformation = entityInformation;
  }

  @Override
  public @NonNull Optional<T> findById(@NonNull ID id) {
    return filterByTenant(super.findById(Objects.requireNonNull(id, "id must not be null")));
  }

  @Override
  public @NonNull List<T> findAllById(@NonNull Iterable<ID> ids) {
    Iterable<ID> safeIds = Objects.requireNonNull(ids, "ids must not be null");
    List<T> results = super.findAllById(safeIds);
    if (!isTenantFilteredEntity() || results.isEmpty()) {
      return results;
    }
    List<T> filtered = new ArrayList<>(results.size());
    for (T entity : results) {
      if (isEntityVisible(entity)) {
        filtered.add(entity);
      }
    }
    return filtered;
  }

  @Override
  public @NonNull T getReferenceById(@NonNull ID id) {
    T entity = super.getReferenceById(Objects.requireNonNull(id, "id must not be null"));
    ensureVisible(entity);
    return entity;
  }

  @Override
  public @NonNull T getById(@NonNull ID id) {
    return getReferenceById(id);
  }

  @Override
  public @NonNull T getOne(@NonNull ID id) {
    return getReferenceById(id);
  }

  @Override
  public boolean existsById(@NonNull ID id) {
    Objects.requireNonNull(id, "id must not be null");
    if (!isTenantFilteredEntity()) {
      return super.existsById(id);
    }
    return findById(id).isPresent();
  }

  @Override
  public void deleteById(@NonNull ID id) {
    Objects.requireNonNull(id, "id must not be null");
    if (!isTenantFilteredEntity()) {
      super.deleteById(id);
      return;
    }
    findById(id).ifPresent(super::delete);
  }

  @Override
  public void delete(@NonNull T entity) {
    Objects.requireNonNull(entity, "entity must not be null");
    ensureVisible(entity);
    super.delete(entity);
  }

  @Override
  public void deleteAll(@NonNull Iterable<? extends T> entities) {
    Iterable<? extends T> safeEntities = Objects.requireNonNull(entities, "entities must not be null");
    if (!isTenantFilteredEntity()) {
      super.deleteAll(safeEntities);
      return;
    }
    for (T entity : safeEntities) {
      delete(entity);
    }
  }

  @Override
  public void deleteAllById(@NonNull Iterable<? extends ID> ids) {
    Iterable<? extends ID> safeIds = Objects.requireNonNull(ids, "ids must not be null");
    if (!isTenantFilteredEntity()) {
      super.deleteAllById(safeIds);
      return;
    }
    for (ID id : safeIds) {
      deleteById(id);
    }
  }

  private Optional<T> filterByTenant(Optional<T> candidate) {
    if (candidate.isEmpty() || !isTenantFilteredEntity()) {
      return candidate;
    }
    T entity = candidate.get();
    return isEntityVisible(entity) ? candidate : Optional.empty();
  }

  private void ensureVisible(T entity) {
    if (entity == null || !isTenantFilteredEntity()) {
      return;
    }
    if (!isEntityVisible(entity)) {
      throw new EntityNotFoundException(entityInformation.getJavaType().getSimpleName() + " not found");
    }
  }

  private boolean isEntityVisible(T entity) {
    if (entity == null) {
      return false;
    }
    if (!isTenantFilteredEntity()) {
      return true;
    }
    UUID currentTenant = TenantContext.getTenantId();
    if (currentTenant == null) {
      logger.debug("Tenant context missing while accessing {} - skipping tenant guard",
          entityInformation.getJavaType().getSimpleName());
      return true;
    }
    UUID entityTenant = TenantIntrospector.resolveTenantId(entity);
    if (entityTenant == null) {
      logger.warn("Blocking access to {} because tenant identifier is absent", entityInformation.getJavaType().getName());
      return false;
    }
    boolean visible = currentTenant.equals(entityTenant);
    if (!visible) {
      logger.debug("Entity {}#{} belongs to tenant {} but current tenant is {}",
          entityInformation.getJavaType().getSimpleName(),
          entityInformation.getId(entity),
          entityTenant,
          currentTenant);
    }
    return visible;
  }

  private boolean isTenantFilteredEntity() {
    return entityInformation.getJavaType().isAnnotationPresent(TenantFiltered.class);
  }
}
