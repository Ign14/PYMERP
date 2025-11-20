package com.datakomerz.pymes.common;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Centraliza la normalización de valores de entrada para evitar heurísticas dispersas.
 */
@Component
public class ValueNormalizer {

  /**
   * Trim y convierte cadenas vacías en {@code null}.
   */
  @Nullable
  public String normalize(@Nullable String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  /**
   * Variante estática para escenarios utilitarios donde no se dispone de un bean.
   */
  @Nullable
  public static String normalizeOrNull(@Nullable String value) {
    return (value != null && StringUtils.hasText(value)) ? value.trim() : null;
  }

  /**
   * Normaliza términos de búsqueda permitiendo cadenas vacías (devuelve {@code null} cuando quedan vacías tras el trim).
   */
  @Nullable
  public String normalizeSearch(@Nullable String value) {
    String normalized = normalize(value);
    return (normalized != null && normalized.isEmpty()) ? null : normalized;
  }

  /**
   * Normaliza segmentos, eliminando el código de "sin segmentar" si se reconoce.
   */
  @Nullable
  public String normalizeSegment(@Nullable String segment, @Nullable String unassignedSegmentCode) {
    String normalized = normalize(segment);
    if (normalized == null) {
      return null;
    }
    if (unassignedSegmentCode != null && normalized.equalsIgnoreCase(unassignedSegmentCode.trim())) {
      return null;
    }
    return normalized;
  }

  /**
   * Normaliza emails dejándolos en minúsculas.
   */
  @Nullable
  public String normalizeEmail(@Nullable String value) {
    String normalized = normalize(value);
    return normalized != null ? normalized.toLowerCase() : null;
  }
}
