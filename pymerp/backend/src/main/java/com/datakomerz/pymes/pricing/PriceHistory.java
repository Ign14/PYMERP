package com.datakomerz.pymes.pricing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "price_history")
public class PriceHistory {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Column(name = "product_id", nullable = false, columnDefinition = "uuid")
  private UUID productId;

  @Column(nullable = false, precision = 14, scale = 4)
  private BigDecimal price;

  @Column(name = "valid_from", nullable = false)
  private OffsetDateTime validFrom;

  @PrePersist
  public void prePersist() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (validFrom == null) {
      validFrom = OffsetDateTime.now();
    }
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getProductId() {
    return productId;
  }

  public void setProductId(UUID productId) {
    this.productId = productId;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public OffsetDateTime getValidFrom() {
    return validFrom;
  }

  public void setValidFrom(OffsetDateTime validFrom) {
    this.validFrom = validFrom;
  }
}
