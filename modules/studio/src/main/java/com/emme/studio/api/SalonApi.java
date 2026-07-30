package com.emme.studio.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API for the Salon module. Other modules depend on this interface, never on salon.entity
 * classes.
 */
public interface SalonApi {
  Optional<BusinessProfileInfo> getBusinessProfile(UUID tenantId);

  /** List appointments for a tenant, ordered by start time descending. */
  List<AppointmentInfo> listAppointments(UUID tenantId);

  /** List customers for a tenant. */
  List<CustomerInfo> listCustomers(UUID tenantId);
}
