package com.datakomerz.pymes.multitenancy;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.type.descriptor.java.UUIDJavaType;

import java.util.UUID;

/**
 * Base class for entities that require automatic tenant filtering.
 * Entities extending this class will automatically have tenant isolation
 * applied via Hibernate filters.
 */
@MappedSuperclass
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUIDJavaType.class))
@Filter(name = "tenantFilter", condition = "company_id = :tenantId")
public abstract class TenantAwareEntity {

    @Column(name = "company_id", nullable = false, columnDefinition = "uuid")
    private UUID companyId;

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }
}
