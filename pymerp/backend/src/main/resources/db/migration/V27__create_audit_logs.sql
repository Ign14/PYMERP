-- V27: Crear tabla de audit_logs para registro de auditoría
-- Cumple con ISO 27001, SOC 2, GDPR
-- Autor: Sistema de auditoría
-- Fecha: 2025

CREATE TABLE audit_logs (
  id BIGSERIAL PRIMARY KEY,
  timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
  username VARCHAR(100) NOT NULL,
  user_roles VARCHAR(200),
  action VARCHAR(50) NOT NULL,
  entity_type VARCHAR(100),
  entity_id BIGINT,
  http_method VARCHAR(10),
  endpoint VARCHAR(500),
  ip_address VARCHAR(45),
  user_agent VARCHAR(500),
  company_id BIGINT,
  status_code INTEGER,
  error_message VARCHAR(1000),
  request_body VARCHAR(4000),
  response_time_ms BIGINT
);

-- Índices para optimizar consultas frecuentes
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp DESC);
CREATE INDEX idx_audit_username ON audit_logs(username);
CREATE INDEX idx_audit_company ON audit_logs(company_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_entity_type ON audit_logs(entity_type);
CREATE INDEX idx_audit_ip ON audit_logs(ip_address);

-- Índice compuesto para filtros comunes
CREATE INDEX idx_audit_company_timestamp ON audit_logs(company_id, timestamp DESC);
CREATE INDEX idx_audit_username_timestamp ON audit_logs(username, timestamp DESC);

-- Comentarios descriptivos
COMMENT ON TABLE audit_logs IS 'Registro de auditoría para compliance (ISO 27001, SOC 2, GDPR)';
COMMENT ON COLUMN audit_logs.timestamp IS 'Timestamp UTC de la acción';
COMMENT ON COLUMN audit_logs.username IS 'Usuario que ejecutó la acción';
COMMENT ON COLUMN audit_logs.user_roles IS 'Roles del usuario (separados por coma)';
COMMENT ON COLUMN audit_logs.action IS 'Acción: CREATE, READ, UPDATE, DELETE, LOGIN, LOGOUT, ACCESS_DENIED';
COMMENT ON COLUMN audit_logs.entity_type IS 'Tipo de entidad: Customer, Product, Sale, etc.';
COMMENT ON COLUMN audit_logs.entity_id IS 'ID de la entidad afectada';
COMMENT ON COLUMN audit_logs.http_method IS 'Método HTTP: GET, POST, PUT, DELETE';
COMMENT ON COLUMN audit_logs.endpoint IS 'Endpoint llamado';
COMMENT ON COLUMN audit_logs.ip_address IS 'Dirección IP del cliente';
COMMENT ON COLUMN audit_logs.user_agent IS 'User-Agent del navegador';
COMMENT ON COLUMN audit_logs.company_id IS 'ID de la empresa (multi-tenancy)';
COMMENT ON COLUMN audit_logs.status_code IS 'Código de respuesta HTTP';
COMMENT ON COLUMN audit_logs.error_message IS 'Mensaje de error (solo para requests fallidos)';
COMMENT ON COLUMN audit_logs.request_body IS 'Request body JSON (máx 4000 chars, NO almacenar passwords)';
COMMENT ON COLUMN audit_logs.response_time_ms IS 'Tiempo de respuesta en millisegundos';
