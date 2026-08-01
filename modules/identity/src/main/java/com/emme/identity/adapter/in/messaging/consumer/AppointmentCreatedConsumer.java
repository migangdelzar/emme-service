package com.emme.identity.adapter.in.messaging.consumer;

import com.emme.identity.api.command.EnsureCustomerMembershipCommand;
import com.emme.identity.api.usecase.EnsureCustomerMembershipUseCase;
import com.emme.studio.api.event.AppointmentCreatedEvent;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Starts customer membership establishment when a customer creates an appointment. */
@Component
public class AppointmentCreatedConsumer {

  private final EnsureCustomerMembershipUseCase ensureCustomerMembership;

  public AppointmentCreatedConsumer(EnsureCustomerMembershipUseCase ensureCustomerMembership) {
    this.ensureCustomerMembership = ensureCustomerMembership;
  }

  @EventListener
  public void on(AppointmentCreatedEvent event) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
      return;
    }
    if (!"CUSTOMER".equals(jwt.getClaimAsString("role"))) {
      return;
    }

    UUID customerId = UUID.fromString(jwt.getSubject());
    ensureCustomerMembership.ensure(
        new EnsureCustomerMembershipCommand(customerId, event.tenantId()));
  }
}
