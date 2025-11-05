package com.datakomerz.pymes.audit;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entidad de auditoría para registrar todas las acciones en el sistema.
 * Cumple con requisitos de compliance (ISO 27001, SOC 2, GDPR).
 */
@Entity
@Table(name = "audit_logs", indexes = {
  @Index(name = "idx_audit_timestamp", columnList = "timestamp DESC"),
  @Index(name = "idx_audit_username", columnList = "username"),
  @Index(name = "idx_audit_company", columnList = "company_id"),
  @Index(name = "idx_audit_action", columnList = "action"),
  @Index(name = "idx_audit_entity_type", columnList = "entity_type"),
  @Index(name = "idx_audit_ip", columnList = "ip_address")
})
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Timestamp de la acción (UTC)
   */
  @Column(nullable = false)
  private Instant timestamp;

  /**
   * Username del usuario que ejecutó la acción
   */
  @Column(nullable = false, length = 100)
  private String username;

  /**
   * Roles del usuario al momento de la acción (separados por coma)
   * Ejemplo: "ROLE_ADMIN,ROLE_ERP_USER"
   */
  @Column(name = "user_roles", length = 200)
  private String userRoles;

  /**
   * Acción ejecutada: CREATE, READ, UPDATE, DELETE, LOGIN, LOGOUT, ACCESS_DENIED
   */
  @Column(nullable = false, length = 50)
  private String action;

  /**
   * Tipo de entidad afectada: Customer, Product, Sale, Purchase, etc.
   */
  @Column(name = "entity_type", length = 100)
  private String entityType;

  /**
   * ID de la entidad afectada (nullable para acciones genéricas)
   */
  @Column(name = "entity_id")
  private Long entityId;

  /**
   * Método HTTP: GET, POST, PUT, DELETE, PATCH
   */
  @Column(name = "http_method", length = 10)
  private String httpMethod;

  /**
   * Endpoint llamado: /api/v1/customers/123
   */
  @Column(length = 500)
  private String endpoint;

  /**
   * Dirección IP del cliente
   */
  @Column(name = "ip_address", length = 45)
  private String ipAddress;

  /**
   * User-Agent del navegador/cliente
   */
  @Column(name = "user_agent", length = 500)
  private String userAgent;

  /**
   * Company ID (multi-tenancy)
   */
  @Column(name = "company_id")
  private Long companyId;

  /**
   * Código de respuesta HTTP: 200, 201, 403, 401, 400, 500, etc.
   */
  @Column(name = "status_code")
  private Integer statusCode;

  /**
   * Mensaje de error (solo para requests fallidos)
   */
  @Column(name = "error_message", length = 1000)
  private String errorMessage;

  /**
   * Request body (JSON) - solo para POST/PUT
   * Limitado a 4000 caracteres para evitar overhead
   * NOTA: No almacenar passwords o tokens
   */
  @Column(name = "request_body", length = 4000)
  private String requestBody;

  /**
   * Tiempo de respuesta en millisegundos
   */
  @Column(name = "response_time_ms")
  private Long responseTimeMs;

  // Constructors
  public AuditLog() {
    this.timestamp = Instant.now();
  }

  // Getters and Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getUserRoles() {
    return userRoles;
  }

  public void setUserRoles(String userRoles) {
    this.userRoles = userRoles;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getEntityType() {
    return entityType;
  }

  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  public Long getEntityId() {
    return entityId;
  }

  public void setEntityId(Long entityId) {
    this.entityId = entityId;
  }

  public String getHttpMethod() {
    return httpMethod;
  }

  public void setHttpMethod(String httpMethod) {
    this.httpMethod = httpMethod;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public void setCompanyId(Long companyId) {
    this.companyId = companyId;
  }

  public Integer getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(Integer statusCode) {
    this.statusCode = statusCode;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getRequestBody() {
    return requestBody;
  }

  public void setRequestBody(String requestBody) {
    this.requestBody = requestBody;
  }

  public Long getResponseTimeMs() {
    return responseTimeMs;
  }

  public void setResponseTimeMs(Long responseTimeMs) {
    this.responseTimeMs = responseTimeMs;
  }

  @Override
  public String toString() {
    return "AuditLog{" +
           "id=" + id +
           ", timestamp=" + timestamp +
           ", username='" + username + '\'' +
           ", action='" + action + '\'' +
           ", entityType='" + entityType + '\'' +
           ", entityId=" + entityId +
           ", endpoint='" + endpoint + '\'' +
           ", statusCode=" + statusCode +
           '}';
  }
}
