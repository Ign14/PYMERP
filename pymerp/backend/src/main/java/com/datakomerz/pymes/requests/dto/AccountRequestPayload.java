package com.datakomerz.pymes.requests.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountRequestPayload(
  @NotBlank(message = "El RUT es obligatorio")
  String rut,
  @NotBlank(message = "El nombre completo es obligatorio")
  @Size(max = 120, message = "El nombre completo no puede exceder 120 caracteres")
  String fullName,
  @NotBlank(message = "La dirección es obligatoria")
  @Size(max = 200, message = "La dirección no puede exceder 200 caracteres")
  String address,
  @NotBlank(message = "El email es obligatorio")
  @Email(message = "Email inválido")
  @Size(max = 150, message = "El email no puede exceder 150 caracteres")
  String email,
  @NotBlank(message = "El nombre de la empresa es obligatorio")
  @Size(max = 150, message = "El nombre de la empresa no puede exceder 150 caracteres")
  String companyName,
  @NotBlank(message = "La contraseña es obligatoria")
  @Size(min = 8, max = 120, message = "La contraseña debe tener entre 8 y 120 caracteres")
  String password,
  @NotBlank(message = "Debes confirmar la contraseña")
  String confirmPassword
) {
  @AssertTrue(message = "Las contraseñas no coinciden")
  public boolean isPasswordConfirmed() {
    return password != null && password.equals(confirmPassword);
  }

  public boolean passwordsMatch() {
    return password != null && password.equals(confirmPassword);
  }
}
