package com.datakomerz.pymes.common;

import com.datakomerz.pymes.auth.InvalidRefreshTokenException;
import com.datakomerz.pymes.auth.TenantMismatchException;
import com.datakomerz.pymes.auth.UserDisabledException;
import jakarta.persistence.EntityNotFoundException;
import java.util.Map;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
    var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
    pd.setTitle("Bad Request");
    pd.setProperty("errors", ex.getBindingResult().getFieldErrors());
    return pd;
  }

  @ExceptionHandler(EntityNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ProblemDetail handleNotFound(EntityNotFoundException ex) {
    var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    pd.setTitle("Not Found");
    return pd;
  }

  @ExceptionHandler(BadCredentialsException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
    var pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    pd.setTitle("Unauthorized");
    return pd;
  }

  @ExceptionHandler(InvalidRefreshTokenException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public Map<String, String> handleInvalidRefresh(InvalidRefreshTokenException ex) {
    return Map.of("error", ex.getErrorCode());
  }

  @ExceptionHandler(UserDisabledException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public Map<String, String> handleUserDisabled(UserDisabledException ex) {
    return Map.of("error", ex.getErrorCode());
  }

  @ExceptionHandler(TenantMismatchException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public Map<String, String> handleTenantMismatch(TenantMismatchException ex) {
    return Map.of("error", ex.getErrorCode());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
    var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    pd.setTitle("Bad Request");
    return pd;
  }

  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
    var pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
    pd.setTitle("Forbidden");
    return pd;
  }

  @ExceptionHandler(IllegalStateException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ProblemDetail handleIllegalState(IllegalStateException ex) {
    var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    pd.setTitle("Conflict");
    return pd;
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
    String detail = "Data integrity violation";
    Throwable root = ex.getMostSpecificCause();
    if (root instanceof ConstraintViolationException constraint && constraint.getConstraintName() != null) {
      String name = constraint.getConstraintName();
      if (name.contains("products_company_id_sku")) {
        detail = "A product with this SKU already exists for the company";
      } else {
        detail = constraint.getLocalizedMessage();
      }
    } else if (root != null && root.getMessage() != null) {
      detail = root.getMessage();
    }

    var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
    pd.setTitle("Conflict");
    return pd;
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ProblemDetail handleOther(Exception ex) {
    var pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    pd.setTitle("Internal Server Error");
    return pd;
  }
}
