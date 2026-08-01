package com.emme.identity.adapter.in.messaging.consumer;

import com.emme.identity.application.service.EnsureCustomerMembershipService;
import com.emme.studio.api.event.AppointmentCreatedEvent;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Starts customer membership establishment when a customer creates an appointment. */
@Component
public class AppointmentCreatedConsumer {

  private final EnsureCustomerMembershipService membershipService;

  public AppointmentCreatedConsumer(EnsureCustomerMembershipService membershipService) {
    this.membershipService = membershipService;
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
    membershipService.ensureForCustomer(customerId, event.tenantId());
  }
}
