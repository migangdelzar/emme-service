package com.emme.shared.web.advice;

import com.emme.shared.web.i18n.ProblemDetailFactory;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private final ProblemDetailFactory problems;

  public GlobalExceptionHandler(ProblemDetailFactory problems) {
    this.problems = problems;
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ProblemDetail handleNotFound(IllegalArgumentException ex, WebRequest request) {
    return problems.create(HttpStatus.NOT_FOUND, "NOT_FOUND", language(request));
  }

  @ExceptionHandler(IllegalStateException.class)
  ProblemDetail handleConflict(IllegalStateException ex, WebRequest request) {
    return problems.create(HttpStatus.CONFLICT, "CONFLICT", language(request));
  }

  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail handleAccessDenied(AccessDeniedException ex, WebRequest request) {
    return problems.create(HttpStatus.FORBIDDEN, "FORBIDDEN", language(request));
  }

  @ExceptionHandler(AuthenticationException.class)
  ProblemDetail handleAuthentication(AuthenticationException ex, WebRequest request) {
    return problems.create(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", language(request));
  }

  @ExceptionHandler(InvalidApiVersionException.class)
  ProblemDetail handleInvalidApiVersion(InvalidApiVersionException ex, WebRequest request) {
    return problems.create(HttpStatus.BAD_REQUEST, "API_VERSION_INVALID", language(request));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
    var problem = problems.create(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", language(request));
    problem.setProperty(
        "fieldErrors",
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> Map.of("field", error.getField(), "message", error.getDefaultMessage()))
            .toList());
    return problem;
  }

  private static String language(WebRequest request) {
    return request.getHeader("Accept-Language");
  }

  // Module-specific exceptions (EntitlementViolationException, etc.)
  // are handled by their own module's @RestControllerAdvice.
  // shared has no allowedDependencies — cannot import from other modules.
}
