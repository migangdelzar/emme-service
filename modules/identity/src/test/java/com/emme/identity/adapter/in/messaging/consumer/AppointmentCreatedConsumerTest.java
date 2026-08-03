package com.emme.identity.adapter.in.messaging.consumer;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.emme.identity.api.command.EnsureCustomerMembershipCommand;
import com.emme.identity.api.usecase.EnsureCustomerMembershipUseCase;
import com.emme.studio.api.event.AppointmentCreatedEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class AppointmentCreatedConsumerTest {

  @Mock private EnsureCustomerMembershipUseCase ensureCustomerMembership;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void delegatesCustomerAppointmentToMembershipService() {
    UUID customerId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    authenticate(customerId, "CUSTOMER");

    new AppointmentCreatedConsumer(ensureCustomerMembership).on(event(tenantId));

    verify(ensureCustomerMembership)
        .ensure(new EnsureCustomerMembershipCommand(customerId, tenantId));
  }

  @Test
  void ignoresAppointmentsCreatedByNonCustomers() {
    authenticate(UUID.randomUUID(), "STAFF");

    new AppointmentCreatedConsumer(ensureCustomerMembership).on(event(UUID.randomUUID()));

    verify(ensureCustomerMembership, never())
        .ensure(org.mockito.ArgumentMatchers.any(EnsureCustomerMembershipCommand.class));
  }

  private static void authenticate(UUID customerId, String role) {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .subject(customerId.toString())
            .claim("role", role)
            .header("alg", "none")
            .build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
  }

  private static AppointmentCreatedEvent event(UUID tenantId) {
    return new AppointmentCreatedEvent(
        UUID.randomUUID(),
        tenantId,
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        Instant.parse("2026-01-01T10:00:00Z"),
        Instant.parse("2026-01-01T11:00:00Z"),
        Instant.parse("2026-01-01T09:00:00Z"));
  }
}
