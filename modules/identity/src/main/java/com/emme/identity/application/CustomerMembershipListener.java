package com.emme.identity.application;

import com.emme.identity.entity.CustomerMembership;
import com.emme.identity.entity.CustomerMembershipRepository;
import com.emme.studio.api.event.AppointmentCreatedEvent;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Auto-creates a customer membership record when a customer creates their first appointment at a
 * tenant. This links the global customer identity to the tenant so the customer appears in the
 * business dashboard.
 */
@Component
public class CustomerMembershipListener {

  private static final Logger log = LoggerFactory.getLogger(CustomerMembershipListener.class);

  private final CustomerMembershipRepository membershipRepo;

  public CustomerMembershipListener(CustomerMembershipRepository membershipRepo) {
    this.membershipRepo = membershipRepo;
  }

  @EventListener
  public void onAppointmentCreated(AppointmentCreatedEvent event) {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
      return;
    }
    if (!"CUSTOMER".equals(jwt.getClaimAsString("role"))) {
      return;
    }
    UUID customerId = UUID.fromString(jwt.getSubject());
    if (membershipRepo.existsByCustomerIdAndTenantId(customerId, event.tenantId())) {
      return;
    }
    membershipRepo.save(new CustomerMembership(customerId, event.tenantId()));
    log.info("Auto-created membership for customer {} in tenant {}", customerId, event.tenantId());
  }
}
