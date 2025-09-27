package com.datakomerz.pymes.common.captcha;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SimpleCaptchaPayload(
  @NotNull(message = "captcha.a es obligatorio")
  @Min(value = 0, message = "captcha.a debe ser mayor o igual a 0")
  @Max(value = 50, message = "captcha.a excede el máximo permitido")
  Integer a,

  @NotNull(message = "captcha.b es obligatorio")
  @Min(value = 0, message = "captcha.b debe ser mayor o igual a 0")
  @Max(value = 50, message = "captcha.b excede el máximo permitido")
  Integer b,

  @NotBlank(message = "La respuesta del captcha es obligatoria")
  String answer
) {
}
