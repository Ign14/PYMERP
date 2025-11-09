package com.datakomerz.pymes.sales.dto;

import com.datakomerz.pymes.common.captcha.SimpleCaptchaPayload;
import com.datakomerz.pymes.validation.ValidPaymentTerm;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record SaleReq(
  @NotNull UUID customerId,
  String paymentMethod,
  String docType,
  BigDecimal discount,
  BigDecimal vatRate,
  @NotNull @ValidPaymentTerm Integer paymentTermDays,
  @NotEmpty List<@Valid SaleItemReq> items,
  @Valid SimpleCaptchaPayload captcha
) {}
