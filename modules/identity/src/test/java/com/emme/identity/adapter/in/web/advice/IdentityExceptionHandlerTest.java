package com.emme.identity.adapter.in.web.advice;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.identity.api.exception.CustomerNotFoundException;
import com.emme.identity.api.exception.InvalidCustomerTokenException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class IdentityExceptionHandlerTest {

  private final IdentityExceptionHandler handler = new IdentityExceptionHandler();

  @Test
  void mapsInvalidCustomerTokenToUnauthorizedProblemDetail() {
    var problem = handler.handleInvalidCustomerToken(new InvalidCustomerTokenException());

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(problem.getProperties()).containsEntry("code", "IDENTITY_CUSTOMER_TOKEN_INVALID");
  }

  @Test
  void mapsMissingCustomerToNotFoundProblemDetail() {
    var problem = handler.handleCustomerNotFound(new CustomerNotFoundException(UUID.randomUUID()));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(problem.getProperties()).containsEntry("code", "IDENTITY_CUSTOMER_NOT_FOUND");
  }
}
