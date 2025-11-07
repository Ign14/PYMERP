package com.datakomerz.pymes.multitenancy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca una entidad JPA como filtrable por tenant.
 * 
 * <p>Las entidades marcadas con esta anotación serán automáticamente filtradas
 * por company_id en todas las consultas, proporcionando aislamiento de datos
 * a nivel de base de datos.</p>
 * 
 * <p><strong>Requisitos de la entidad:</strong></p>
 * <ul>
 *   <li>Debe tener un campo llamado "companyId" de tipo UUID</li>
 *   <li>Debe tener anotaciones @FilterDef y @Filter de Hibernate</li>
 * </ul>
 * 
 * <p><strong>Ejemplo de uso:</strong></p>
 * <pre>
 * {@code @Entity}
 * {@code @Table(name = "products")}
 * {@code @TenantFiltered}
 * {@code @FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = "uuid"))}
 * {@code @Filter(name = "tenantFilter", condition = "company_id = :tenantId")}
 * public class Product {
 *     
 *     {@code @Column(name = "company_id", nullable = false)}
 *     private UUID companyId;
 *     
 *     // ... otros campos
 * }
 * </pre>
 * 
 * <p>El filtro se aplica automáticamente en todas las queries:</p>
 * <pre>
 * // Query generado sin filtro:
 * SELECT * FROM products WHERE name LIKE '%laptop%'
 * 
 * // Query con filtro automático aplicado:
 * SELECT * FROM products WHERE name LIKE '%laptop%' AND company_id = '550e8400-...'
 * </pre>
 * 
 * @see org.hibernate.annotations.FilterDef
 * @see org.hibernate.annotations.Filter
 * @since Sprint 5
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TenantFiltered {
    
    /**
     * Nombre de la columna que contiene el tenant ID en la base de datos.
     * Por defecto es "company_id".
     * 
     * @return nombre de la columna de tenant
     */
    String tenantColumn() default "company_id";
}
