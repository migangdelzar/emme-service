package com.emme.identity.adapter.in.web.advice;

import com.emme.identity.api.exception.CustomerNotFoundException;
import com.emme.identity.api.exception.InvalidCustomerTokenException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps Identity-owned expected failures to stable RFC 9457 problem details. */
@RestControllerAdvice(basePackages = "com.emme.identity.adapter.in.web.controller")
@Order(0)
public class IdentityExceptionHandler {

  @ExceptionHandler(InvalidCustomerTokenException.class)
  ProblemDetail handleInvalidCustomerToken(InvalidCustomerTokenException exception) {
    return problem(
        HttpStatus.UNAUTHORIZED,
        "Customer token is invalid",
        exception.getMessage(),
        "IDENTITY_CUSTOMER_TOKEN_INVALID");
  }

  @ExceptionHandler(CustomerNotFoundException.class)
  ProblemDetail handleCustomerNotFound(CustomerNotFoundException exception) {
    return problem(
        HttpStatus.NOT_FOUND,
        "Customer not found",
        exception.getMessage(),
        "IDENTITY_CUSTOMER_NOT_FOUND");
  }

  private static ProblemDetail problem(
      HttpStatus status, String title, String detail, String code) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    problem.setProperty("code", code);
    return problem;
  }
}
