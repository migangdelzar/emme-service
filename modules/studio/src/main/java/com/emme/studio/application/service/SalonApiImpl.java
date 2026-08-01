package com.emme.studio.application.service;

import com.emme.studio.api.result.AppointmentInfo;
import com.emme.studio.api.result.BusinessProfileInfo;
import com.emme.studio.api.result.CustomerInfo;
import com.emme.studio.api.usecase.SalonApi;
import com.emme.studio.entity.AppointmentRepository;
import com.emme.studio.entity.BusinessProfileRepository;
import com.emme.studio.entity.CustomerRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class SalonApiImpl implements SalonApi {

  private final BusinessProfileRepository profileRepository;
  private final AppointmentRepository appointmentRepository;
  private final CustomerRepository customerRepository;

  SalonApiImpl(
      BusinessProfileRepository profileRepository,
      AppointmentRepository appointmentRepository,
      CustomerRepository customerRepository) {
    this.profileRepository = profileRepository;
    this.appointmentRepository = appointmentRepository;
    this.customerRepository = customerRepository;
  }

  @Override
  public Optional<BusinessProfileInfo> getBusinessProfile(UUID tenantId) {
    return profileRepository
        .findByTenantId(tenantId)
        .map(p -> new BusinessProfileInfo(p.getTenantId(), p.getDisplayName(), p.getLocale()));
  }

  @Override
  public List<AppointmentInfo> listAppointments(UUID tenantId) {
    return appointmentRepository.findByTenantIdOrderByStartsAtDesc(tenantId).stream()
        .map(
            a ->
                new AppointmentInfo(
                    a.getId(),
                    a.getStartsAt(),
                    a.getEndsAt(),
                    a.getCustomer() != null ? a.getCustomer().getName() : null,
                    a.getService() != null ? a.getService().getName() : null,
                    a.getArtist() != null ? a.getArtist().getName() : null,
                    a.getStatus() != null ? a.getStatus().name() : null))
        .toList();
  }

  @Override
  public List<CustomerInfo> listCustomers(UUID tenantId) {
    return customerRepository.findByTenantId(tenantId).stream()
        .map(c -> new CustomerInfo(c.getId(), c.getName(), c.getPhone(), c.getEmail()))
        .toList();
  }
}
