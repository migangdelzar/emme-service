package com.emme.studio.application.service;

import com.emme.studio.api.result.AppointmentInfo;
import com.emme.studio.api.result.BusinessProfileInfo;
import com.emme.studio.api.result.CustomerInfo;
import com.emme.studio.api.usecase.SalonApi;
import com.emme.studio.application.port.out.AppointmentRepository;
import com.emme.studio.application.port.out.ArtistRepository;
import com.emme.studio.application.port.out.BusinessProfileRepository;
import com.emme.studio.application.port.out.CustomerRepository;
import com.emme.studio.application.port.out.ServiceRepository;
import com.emme.studio.domain.model.Appointment;
import com.emme.studio.domain.model.Customer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
@Transactional(readOnly = true)
class SalonApiImpl implements SalonApi {

  private final BusinessProfileRepository profileRepository;
  private final AppointmentRepository appointmentRepository;
  private final CustomerRepository customerRepository;
  private final ServiceRepository serviceRepository;
  private final ArtistRepository artistRepository;

  public SalonApiImpl(
      BusinessProfileRepository profileRepository,
      AppointmentRepository appointmentRepository,
      CustomerRepository customerRepository,
      ServiceRepository serviceRepository,
      ArtistRepository artistRepository) {
    this.profileRepository = profileRepository;
    this.appointmentRepository = appointmentRepository;
    this.customerRepository = customerRepository;
    this.serviceRepository = serviceRepository;
    this.artistRepository = artistRepository;
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
        .map(this::toAppointmentInfo)
        .toList();
  }

  @Override
  public List<CustomerInfo> listCustomers(UUID tenantId) {
    return customerRepository.findByTenantId(tenantId).stream()
        .map(c -> new CustomerInfo(c.getId(), c.getName(), c.getPhone(), c.getEmail()))
        .toList();
  }

  private AppointmentInfo toAppointmentInfo(Appointment appointment) {
    String customerName =
        customerRepository
            .findById(appointment.getCustomerId())
            .map(Customer::getName)
            .orElse(null);
    String serviceName =
        serviceRepository
            .findById(appointment.getServiceId())
            .map(com.emme.studio.domain.model.Service::getName)
            .orElse(null);
    String artistName =
        artistRepository
            .findById(appointment.getArtistId())
            .map(com.emme.studio.domain.model.Artist::getName)
            .orElse(null);
    return new AppointmentInfo(
        appointment.getId(),
        appointment.getStartsAt(),
        appointment.getEndsAt(),
        customerName,
        serviceName,
        artistName,
        appointment.getStatus().name());
  }
}
