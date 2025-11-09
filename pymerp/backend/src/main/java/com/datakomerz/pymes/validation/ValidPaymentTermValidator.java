package com.datakomerz.pymes.validation;

import com.datakomerz.pymes.finances.PaymentTerm;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validador para términos de pago/cobro.
 * Verifica que el valor sea uno de: 7, 15, 30, 60 días.
 */
public class ValidPaymentTermValidator implements ConstraintValidator<ValidPaymentTerm, Integer> {

  @Override
  public void initialize(ValidPaymentTerm constraintAnnotation) {
    // No se necesita inicialización
  }

  @Override
  public boolean isValid(Integer value, ConstraintValidatorContext context) {
    if (value == null) {
      return true; // null se valida con @NotNull separado
    }
    
    return PaymentTerm.isValid(value);
  }
}
