package com.emme.identity.adapter.in.messaging.consumer;

import static org.mockito.Mockito.verify;

import com.emme.appointments.api.event.AppointmentCreated;
import com.emme.identity.api.command.EnsureCustomerMembershipCommand;
import com.emme.identity.api.usecase.EnsureCustomerMembershipUseCase;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.kernel.tracing.CorrelationContextHolder;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentCreatedConsumerTest {

  @Mock private EnsureCustomerMembershipUseCase ensureCustomerMembership;

  @Test
  void delegatesExternalizedAppointmentUsingEventCustomerWithoutSecurityContext() {
    UUID customerId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    AppointmentCreated event = event(tenantId, customerId);
    Mockito.doAnswer(
            invocation -> {
              org.assertj.core.api.Assertions.assertThat(
                      TenantContextHolder.currentTenantOptional())
                  .contains(tenantId);
              org.assertj.core.api.Assertions.assertThat(
                      CorrelationContextHolder.requireCorrelationId())
                  .isEqualTo("appointment-created:" + event.eventId());
              return null;
            })
        .when(ensureCustomerMembership)
        .ensure(new EnsureCustomerMembershipCommand(customerId, tenantId));

    new AppointmentCreatedConsumer(ensureCustomerMembership).on(event);

    verify(ensureCustomerMembership)
        .ensure(new EnsureCustomerMembershipCommand(customerId, tenantId));
  }

  private static AppointmentCreated event(UUID tenantId, UUID customerId) {
    return new AppointmentCreated(
        UUID.randomUUID(),
        tenantId,
        UUID.randomUUID(),
        customerId,
        UUID.randomUUID(),
        UUID.randomUUID(),
        Instant.parse("2026-01-01T10:00:00Z"),
        Instant.parse("2026-01-01T11:00:00Z"),
        Instant.parse("2026-01-01T09:00:00Z"));
  }
}
