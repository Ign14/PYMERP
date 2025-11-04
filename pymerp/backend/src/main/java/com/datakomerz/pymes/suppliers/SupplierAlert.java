package com.datakomerz.pymes.suppliers;

import java.util.UUID;

public class SupplierAlert {
  public enum AlertType {
    NO_RECENT_PURCHASES,
    INACTIVE_SUPPLIER,
    HIGH_CONCENTRATION,
    SINGLE_SOURCE
  }

  public enum Severity {
    INFO,
    WARNING,
    CRITICAL
  }

  private UUID supplierId;
  private String supplierName;
  private AlertType type;
  private Severity severity;
  private String message;
  private String actionLabel;
  private Long daysWithoutPurchases;
  private Double concentrationPercentage;

  // Constructor vacío
  public SupplierAlert() {}

  // Constructor completo
  public SupplierAlert(UUID supplierId, String supplierName, AlertType type, Severity severity,
                       String message, String actionLabel, Long daysWithoutPurchases,
                       Double concentrationPercentage) {
    this.supplierId = supplierId;
    this.supplierName = supplierName;
    this.type = type;
    this.severity = severity;
    this.message = message;
    this.actionLabel = actionLabel;
    this.daysWithoutPurchases = daysWithoutPurchases;
    this.concentrationPercentage = concentrationPercentage;
  }

  // Getters & Setters
  public UUID getSupplierId() { return supplierId; }
  public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }

  public String getSupplierName() { return supplierName; }
  public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

  public AlertType getType() { return type; }
  public void setType(AlertType type) { this.type = type; }

  public Severity getSeverity() { return severity; }
  public void setSeverity(Severity severity) { this.severity = severity; }

  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }

  public String getActionLabel() { return actionLabel; }
  public void setActionLabel(String actionLabel) { this.actionLabel = actionLabel; }

  public Long getDaysWithoutPurchases() { return daysWithoutPurchases; }
  public void setDaysWithoutPurchases(Long daysWithoutPurchases) { this.daysWithoutPurchases = daysWithoutPurchases; }

  public Double getConcentrationPercentage() { return concentrationPercentage; }
  public void setConcentrationPercentage(Double concentrationPercentage) { this.concentrationPercentage = concentrationPercentage; }
}
