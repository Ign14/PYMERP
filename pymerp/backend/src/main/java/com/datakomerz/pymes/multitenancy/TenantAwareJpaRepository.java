package com.datakomerz.pymes.multitenancy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

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
  public Optional<T> findById(ID id) {
    return filterByTenant(super.findById(id));
  }

  @Override
  public List<T> findAllById(Iterable<ID> ids) {
    List<T> results = super.findAllById(ids);
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
  public T getReferenceById(ID id) {
    T entity = super.getReferenceById(id);
    ensureVisible(entity);
    return entity;
  }

  @Override
  public T getById(ID id) {
    return getReferenceById(id);
  }

  @Override
  public T getOne(ID id) {
    return getReferenceById(id);
  }

  @Override
  public boolean existsById(ID id) {
    if (!isTenantFilteredEntity()) {
      return super.existsById(id);
    }
    return findById(id).isPresent();
  }

  @Override
  public void deleteById(ID id) {
    if (!isTenantFilteredEntity()) {
      super.deleteById(id);
      return;
    }
    findById(id).ifPresent(super::delete);
  }

  @Override
  public void delete(T entity) {
    if (entity == null) {
      return;
    }
    ensureVisible(entity);
    super.delete(entity);
  }

  @Override
  public void deleteAll(Iterable<? extends T> entities) {
    if (!isTenantFilteredEntity()) {
      super.deleteAll(entities);
      return;
    }
    for (T entity : entities) {
      delete(entity);
    }
  }

  @Override
  public void deleteAllById(Iterable<? extends ID> ids) {
    if (!isTenantFilteredEntity()) {
      super.deleteAllById(ids);
      return;
    }
    for (ID id : ids) {
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
