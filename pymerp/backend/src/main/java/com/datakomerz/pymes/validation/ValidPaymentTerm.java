package com.datakomerz.pymes.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validación para términos de pago/cobro.
 * Solo permite valores: 7, 15, 30, 60 días.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidPaymentTermValidator.class)
@Documented
public @interface ValidPaymentTerm {
  String message() default "Término de pago inválido. Valores permitidos: 7, 15, 30, 60 días";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}
