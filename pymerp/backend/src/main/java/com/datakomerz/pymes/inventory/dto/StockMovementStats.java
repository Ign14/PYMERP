package com.datakomerz.pymes.inventory.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Estadísticas de movimientos de inventario (últimos 30 días)
 */
public class StockMovementStats {
  
  private BigDecimal totalInflows;
  private BigDecimal totalOutflows;
  private Long inflowTransactions;
  private Long outflowTransactions;
  private List<ProductMovement> topInflowProducts;
  private List<ProductMovement> topOutflowProducts;
  private List<CategoryVelocity> categoryVelocities;
  
  public StockMovementStats() {
  }

  public StockMovementStats(BigDecimal totalInflows, BigDecimal totalOutflows, 
                            Long inflowTransactions, Long outflowTransactions,
                            List<ProductMovement> topInflowProducts, 
                            List<ProductMovement> topOutflowProducts,
                            List<CategoryVelocity> categoryVelocities) {
    this.totalInflows = totalInflows;
    this.totalOutflows = totalOutflows;
    this.inflowTransactions = inflowTransactions;
    this.outflowTransactions = outflowTransactions;
    this.topInflowProducts = topInflowProducts;
    this.topOutflowProducts = topOutflowProducts;
    this.categoryVelocities = categoryVelocities;
  }

  public BigDecimal getTotalInflows() {
    return totalInflows;
  }

  public void setTotalInflows(BigDecimal totalInflows) {
    this.totalInflows = totalInflows;
  }

  public BigDecimal getTotalOutflows() {
    return totalOutflows;
  }

  public void setTotalOutflows(BigDecimal totalOutflows) {
    this.totalOutflows = totalOutflows;
  }

  public Long getInflowTransactions() {
    return inflowTransactions;
  }

  public void setInflowTransactions(Long inflowTransactions) {
    this.inflowTransactions = inflowTransactions;
  }

  public Long getOutflowTransactions() {
    return outflowTransactions;
  }

  public void setOutflowTransactions(Long outflowTransactions) {
    this.outflowTransactions = outflowTransactions;
  }

  public List<ProductMovement> getTopInflowProducts() {
    return topInflowProducts;
  }

  public void setTopInflowProducts(List<ProductMovement> topInflowProducts) {
    this.topInflowProducts = topInflowProducts;
  }

  public List<ProductMovement> getTopOutflowProducts() {
    return topOutflowProducts;
  }

  public void setTopOutflowProducts(List<ProductMovement> topOutflowProducts) {
    this.topOutflowProducts = topOutflowProducts;
  }

  public List<CategoryVelocity> getCategoryVelocities() {
    return categoryVelocities;
  }

  public void setCategoryVelocities(List<CategoryVelocity> categoryVelocities) {
    this.categoryVelocities = categoryVelocities;
  }

  /**
   * Movimiento de un producto específico
   */
  public static class ProductMovement {
    private String productId;
    private String productName;
    private BigDecimal quantity;
    private BigDecimal value;
    private Long transactionCount;

    public ProductMovement() {
    }

    public ProductMovement(String productId, String productName, BigDecimal quantity, 
                          BigDecimal value, Long transactionCount) {
      this.productId = productId;
      this.productName = productName;
      this.quantity = quantity;
      this.value = value;
      this.transactionCount = transactionCount;
    }

    public String getProductId() {
      return productId;
    }

    public void setProductId(String productId) {
      this.productId = productId;
    }

    public String getProductName() {
      return productName;
    }

    public void setProductName(String productName) {
      this.productName = productName;
    }

    public BigDecimal getQuantity() {
      return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
      this.quantity = quantity;
    }

    public BigDecimal getValue() {
      return value;
    }

    public void setValue(BigDecimal value) {
      this.value = value;
    }

    public Long getTransactionCount() {
      return transactionCount;
    }

    public void setTransactionCount(Long transactionCount) {
      this.transactionCount = transactionCount;
    }
  }

  /**
   * Velocidad de movimiento por categoría
   */
  public static class CategoryVelocity {
    private String category;
    private BigDecimal averageDailyOutflow;
    private BigDecimal turnoverRate;
    private Long productCount;

    public CategoryVelocity() {
    }

    public CategoryVelocity(String category, BigDecimal averageDailyOutflow, 
                           BigDecimal turnoverRate, Long productCount) {
      this.category = category;
      this.averageDailyOutflow = averageDailyOutflow;
      this.turnoverRate = turnoverRate;
      this.productCount = productCount;
    }

    public String getCategory() {
      return category;
    }

    public void setCategory(String category) {
      this.category = category;
    }

    public BigDecimal getAverageDailyOutflow() {
      return averageDailyOutflow;
    }

    public void setAverageDailyOutflow(BigDecimal averageDailyOutflow) {
      this.averageDailyOutflow = averageDailyOutflow;
    }

    public BigDecimal getTurnoverRate() {
      return turnoverRate;
    }

    public void setTurnoverRate(BigDecimal turnoverRate) {
      this.turnoverRate = turnoverRate;
    }

    public Long getProductCount() {
      return productCount;
    }

    public void setProductCount(Long productCount) {
      this.productCount = productCount;
    }
  }
}
