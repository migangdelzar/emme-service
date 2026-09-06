package com.emme.identity.adapter.in.messaging.consumer;

import com.emme.appointments.api.event.AppointmentCreated;
import com.emme.identity.api.command.EnsureCustomerMembershipCommand;
import com.emme.identity.api.usecase.EnsureCustomerMembershipUseCase;
import com.emme.kernel.context.TenantContextHolder;
import org.springframework.stereotype.Component;

/** Starts idempotent customer membership establishment from the durable appointment fact. */
@Component
public class AppointmentCreatedConsumer {

  private final EnsureCustomerMembershipUseCase ensureCustomerMembership;

  public AppointmentCreatedConsumer(EnsureCustomerMembershipUseCase ensureCustomerMembership) {
    this.ensureCustomerMembership = ensureCustomerMembership;
  }

  @org.springframework.modulith.events.ApplicationModuleListener(
      id = "identity.appointment-created-membership")
  public void on(AppointmentCreated event) {
    TenantContextHolder.withTenantAndCorrelation(
        event.tenantId(),
        "appointment-created:" + event.eventId(),
        () ->
            ensureCustomerMembership.ensure(
                new EnsureCustomerMembershipCommand(event.customerId(), event.tenantId())));
  }
}
