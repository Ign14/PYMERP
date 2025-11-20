package com.datakomerz.pymes.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record PurchaseOrderPayload(
    String orderNumber,
    LocalDate orderDate,
    LocalDate expectedDeliveryDate,
    SupplierInfo supplier,
    CompanyInfo buyer,
    List<PurchaseOrderItem> items,
    BigDecimal subtotal,
    BigDecimal tax,
    BigDecimal total,
    String paymentTerms,
    String deliveryAddress,
    String notes,
    String approvedBy
) {

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String orderNumber;
    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    private SupplierInfo supplier;
    private CompanyInfo buyer;
    private List<PurchaseOrderItem> items = new ArrayList<>();
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal total;
    private String paymentTerms;
    private String deliveryAddress;
    private String notes;
    private String approvedBy;

    public Builder orderNumber(String orderNumber) {
      this.orderNumber = orderNumber;
      return this;
    }

    public Builder orderDate(LocalDate orderDate) {
      this.orderDate = orderDate;
      return this;
    }

    public Builder expectedDeliveryDate(LocalDate expectedDeliveryDate) {
      this.expectedDeliveryDate = expectedDeliveryDate;
      return this;
    }

    public Builder supplier(SupplierInfo supplier) {
      this.supplier = supplier;
      return this;
    }

    public Builder buyer(CompanyInfo buyer) {
      this.buyer = buyer;
      return this;
    }

    public Builder items(List<PurchaseOrderItem> items) {
      if (items == null) {
        this.items = new ArrayList<>();
      } else {
        this.items = new ArrayList<>(items);
      }
      return this;
    }

    public Builder addItem(PurchaseOrderItem item) {
      if (item != null) {
        this.items.add(item);
      }
      return this;
    }

    public Builder subtotal(BigDecimal subtotal) {
      this.subtotal = subtotal;
      return this;
    }

    public Builder tax(BigDecimal tax) {
      this.tax = tax;
      return this;
    }

    public Builder total(BigDecimal total) {
      this.total = total;
      return this;
    }

    public Builder paymentTerms(String paymentTerms) {
      this.paymentTerms = paymentTerms;
      return this;
    }

    public Builder deliveryAddress(String deliveryAddress) {
      this.deliveryAddress = deliveryAddress;
      return this;
    }

    public Builder notes(String notes) {
      this.notes = notes;
      return this;
    }

    public Builder approvedBy(String approvedBy) {
      this.approvedBy = approvedBy;
      return this;
    }

    public PurchaseOrderPayload build() {
      return new PurchaseOrderPayload(
          orderNumber,
          orderDate,
          expectedDeliveryDate,
          supplier,
          buyer,
          List.copyOf(items),
          subtotal,
          tax,
          total,
          paymentTerms,
          deliveryAddress,
          notes,
          approvedBy
      );
    }
  }

  public record SupplierInfo(
      String name,
      String taxId,
      String address,
      String phone,
      String email
  ) {

    public static SupplierInfo.Builder builder() {
      return new SupplierInfo.Builder();
    }

    public static final class Builder {
      private String name;
      private String taxId;
      private String address;
      private String phone;
      private String email;

      public Builder name(String name) {
        this.name = name;
        return this;
      }

      public Builder taxId(String taxId) {
        this.taxId = taxId;
        return this;
      }

      public Builder address(String address) {
        this.address = address;
        return this;
      }

      public Builder phone(String phone) {
        this.phone = phone;
        return this;
      }

      public Builder email(String email) {
        this.email = email;
        return this;
      }

      public SupplierInfo build() {
        return new SupplierInfo(name, taxId, address, phone, email);
      }
    }
  }

  public record CompanyInfo(
      String name,
      String taxId,
      String address,
      String phone,
      String email
  ) {

    public static CompanyInfo.Builder builder() {
      return new CompanyInfo.Builder();
    }

    public static final class Builder {
      private String name;
      private String taxId;
      private String address;
      private String phone;
      private String email;

      public Builder name(String name) {
        this.name = name;
        return this;
      }

      public Builder taxId(String taxId) {
        this.taxId = taxId;
        return this;
      }

      public Builder address(String address) {
        this.address = address;
        return this;
      }

      public Builder phone(String phone) {
        this.phone = phone;
        return this;
      }

      public Builder email(String email) {
        this.email = email;
        return this;
      }

      public CompanyInfo build() {
        return new CompanyInfo(name, taxId, address, phone, email);
      }
    }
  }

  public record PurchaseOrderItem(
      String productCode,
      String description,
      BigDecimal quantity,
      String unit,
      BigDecimal unitPrice,
      BigDecimal subtotal
  ) {

    public static PurchaseOrderItem.Builder builder() {
      return new PurchaseOrderItem.Builder();
    }

    public static final class Builder {
      private String productCode;
      private String description;
      private BigDecimal quantity;
      private String unit;
      private BigDecimal unitPrice;
      private BigDecimal subtotal;

      public Builder productCode(String productCode) {
        this.productCode = productCode;
        return this;
      }

      public Builder description(String description) {
        this.description = description;
        return this;
      }

      public Builder quantity(BigDecimal quantity) {
        this.quantity = quantity;
        return this;
      }

      public Builder unit(String unit) {
        this.unit = unit;
        return this;
      }

      public Builder unitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        return this;
      }

      public Builder subtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
        return this;
      }

      public PurchaseOrderItem build() {
        return new PurchaseOrderItem(
            productCode,
            description,
            quantity,
            unit,
            unitPrice,
            subtotal
        );
      }
    }
  }
}
