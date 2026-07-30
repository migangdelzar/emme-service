package com.emme.shared.web;

import com.emme.kernel.tracing.CorrelationId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  ProblemDetail handleNotFound(IllegalArgumentException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    pd.setProperty("requestId", CorrelationId.get());
    return pd;
  }

  @ExceptionHandler(IllegalStateException.class)
  ProblemDetail handleConflict(IllegalStateException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    pd.setProperty("requestId", CorrelationId.get());
    return pd;
  }

  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail handleAccessDenied(AccessDeniedException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    pd.setProperty("requestId", CorrelationId.get());
    return pd;
  }

  @ExceptionHandler(AuthenticationException.class)
  ProblemDetail handleAuthentication(AuthenticationException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    pd.setProperty("requestId", CorrelationId.get());
    return pd;
  }

  // Module-specific exceptions (EntitlementViolationException, etc.)
  // are handled by their own module's @RestControllerAdvice.
  // shared has no allowedDependencies — cannot import from other modules.
}
