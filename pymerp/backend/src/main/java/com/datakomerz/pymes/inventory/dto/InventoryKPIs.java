package com.datakomerz.pymes.inventory.dto;

import java.math.BigDecimal;

/**
 * KPIs ejecutivos consolidados del inventario
 */
public class InventoryKPIs {
  
  // Cobertura de stock (días promedio que dura el inventario actual)
  private Integer stockCoverageDays;
  
  // Ratio de rotación (veces que el inventario rota por período)
  private BigDecimal turnoverRatio;
  
  // Valor de stock muerto (productos sin movimiento >90 días)
  private BigDecimal deadStockValue;
  
  // Cantidad de productos con stock muerto
  private Long deadStockCount;
  
  // Lead time promedio (días entre pedido y recepción)
  private Integer averageLeadTimeDays;
  
  // Valor total del inventario
  private BigDecimal totalInventoryValue;
  
  // Productos activos en inventario
  private Long activeProducts;
  
  // Productos con stock crítico
  private Long criticalStockProducts;
  
  // Días de inventario disponible (promedio ponderado por valor)
  private Integer daysInventoryOnHand;
  
  // Valor de productos con sobre-stock (>3 meses de cobertura)
  private BigDecimal overstockValue;
  
  // Cantidad de productos con sobre-stock
  private Long overstockCount;

  public InventoryKPIs() {
  }

  public InventoryKPIs(Integer stockCoverageDays, BigDecimal turnoverRatio, BigDecimal deadStockValue, 
                       Long deadStockCount, Integer averageLeadTimeDays, BigDecimal totalInventoryValue,
                       Long activeProducts, Long criticalStockProducts, Integer daysInventoryOnHand,
                       BigDecimal overstockValue, Long overstockCount) {
    this.stockCoverageDays = stockCoverageDays;
    this.turnoverRatio = turnoverRatio;
    this.deadStockValue = deadStockValue;
    this.deadStockCount = deadStockCount;
    this.averageLeadTimeDays = averageLeadTimeDays;
    this.totalInventoryValue = totalInventoryValue;
    this.activeProducts = activeProducts;
    this.criticalStockProducts = criticalStockProducts;
    this.daysInventoryOnHand = daysInventoryOnHand;
    this.overstockValue = overstockValue;
    this.overstockCount = overstockCount;
  }

  public Integer getStockCoverageDays() {
    return stockCoverageDays;
  }

  public void setStockCoverageDays(Integer stockCoverageDays) {
    this.stockCoverageDays = stockCoverageDays;
  }

  public BigDecimal getTurnoverRatio() {
    return turnoverRatio;
  }

  public void setTurnoverRatio(BigDecimal turnoverRatio) {
    this.turnoverRatio = turnoverRatio;
  }

  public BigDecimal getDeadStockValue() {
    return deadStockValue;
  }

  public void setDeadStockValue(BigDecimal deadStockValue) {
    this.deadStockValue = deadStockValue;
  }

  public Long getDeadStockCount() {
    return deadStockCount;
  }

  public void setDeadStockCount(Long deadStockCount) {
    this.deadStockCount = deadStockCount;
  }

  public Integer getAverageLeadTimeDays() {
    return averageLeadTimeDays;
  }

  public void setAverageLeadTimeDays(Integer averageLeadTimeDays) {
    this.averageLeadTimeDays = averageLeadTimeDays;
  }

  public BigDecimal getTotalInventoryValue() {
    return totalInventoryValue;
  }

  public void setTotalInventoryValue(BigDecimal totalInventoryValue) {
    this.totalInventoryValue = totalInventoryValue;
  }

  public Long getActiveProducts() {
    return activeProducts;
  }

  public void setActiveProducts(Long activeProducts) {
    this.activeProducts = activeProducts;
  }

  public Long getCriticalStockProducts() {
    return criticalStockProducts;
  }

  public void setCriticalStockProducts(Long criticalStockProducts) {
    this.criticalStockProducts = criticalStockProducts;
  }

  public Integer getDaysInventoryOnHand() {
    return daysInventoryOnHand;
  }

  public void setDaysInventoryOnHand(Integer daysInventoryOnHand) {
    this.daysInventoryOnHand = daysInventoryOnHand;
  }

  public BigDecimal getOverstockValue() {
    return overstockValue;
  }

  public void setOverstockValue(BigDecimal overstockValue) {
    this.overstockValue = overstockValue;
  }

  public Long getOverstockCount() {
    return overstockCount;
  }

  public void setOverstockCount(Long overstockCount) {
    this.overstockCount = overstockCount;
  }
}
