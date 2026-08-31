package com.emme.appointments.application.service;

import com.emme.appointments.api.result.AvailableSlot;
import com.emme.appointments.api.usecase.FindAvailableSlotsUseCase;
import com.emme.appointments.application.port.out.AppointmentCollisionPort;
import com.emme.salon.application.port.out.OperatingHoursRepository;
import com.emme.salon.domain.model.DayOfWeek;
import com.emme.salon.domain.model.OperatingHours;
import com.emme.services.application.port.out.ArtistCapabilityRepository;
import com.emme.services.application.port.out.ServiceRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Application service for finding available appointment slots. */
@org.springframework.stereotype.Service
@Transactional(readOnly = true)
public class FindAvailableSlotsService implements FindAvailableSlotsUseCase {

  private static final int SLOT_INTERVAL_MINUTES = 15;
  private static final ZoneId STUDIO_ZONE = ZoneId.of("America/Mexico_City");
  private final ArtistCapabilityRepository artistCapabilityRepository;
  private final OperatingHoursRepository operatingHoursRepository;
  private final ServiceRepository serviceRepository;
  private final AppointmentCollisionPort collisionPort;

  public FindAvailableSlotsService(
      ArtistCapabilityRepository artistCapabilityRepository,
      OperatingHoursRepository operatingHoursRepository,
      ServiceRepository serviceRepository,
      AppointmentCollisionPort collisionPort) {
    this.artistCapabilityRepository = artistCapabilityRepository;
    this.operatingHoursRepository = operatingHoursRepository;
    this.serviceRepository = serviceRepository;
    this.collisionPort = collisionPort;
  }

  @Override
  public List<AvailableSlot> find(UUID tenantId, UUID serviceId, LocalDate date) {
    com.emme.services.domain.model.Service service =
        serviceRepository
            .findById(serviceId)
            .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));
    Optional<OperatingHours> hours = findHours(tenantId, date);
    if (hours.isEmpty() || !hours.get().isActive()) {
      return List.of();
    }
    OperatingHours operatingHours = hours.get();
    List<UUID> artistIds =
        artistCapabilityRepository.findByServiceIdAndActive(serviceId).stream()
            .map(capability -> capability.getArtist().getId())
            .toList();
    List<AvailableSlot> slots = new ArrayList<>();
    LocalTime slotStart = operatingHours.getOpensAt();
    while (!slotStart
        .plusMinutes(service.getDurationMinutes())
        .isAfter(operatingHours.getClosesAt())) {
      Instant startsAt = toInstant(date, slotStart);
      Instant endsAt = toInstant(date, slotStart.plusMinutes(service.getDurationMinutes()));
      for (UUID artistId : artistIds) {
        if (!collisionPort.hasCollision(tenantId, artistId, startsAt, endsAt, null)) {
          slots.add(new AvailableSlot(artistId, startsAt, endsAt));
        }
      }
      slotStart = slotStart.plusMinutes(SLOT_INTERVAL_MINUTES);
    }
    return slots;
  }

  private Optional<OperatingHours> findHours(UUID tenantId, LocalDate date) {
    return operatingHoursRepository.findByTenantIdAndDayOfWeek(
        tenantId, toStudioDayOfWeek(date.getDayOfWeek()));
  }

  private DayOfWeek toStudioDayOfWeek(java.time.DayOfWeek javaDay) {
    return switch (javaDay) {
      case MONDAY -> DayOfWeek.MON;
      case TUESDAY -> DayOfWeek.TUE;
      case WEDNESDAY -> DayOfWeek.WED;
      case THURSDAY -> DayOfWeek.THU;
      case FRIDAY -> DayOfWeek.FRI;
      case SATURDAY -> DayOfWeek.SAT;
      case SUNDAY -> DayOfWeek.SUN;
    };
  }

  private Instant toInstant(LocalDate date, LocalTime time) {
    return ZonedDateTime.of(date, time, STUDIO_ZONE).toInstant();
  }
}
