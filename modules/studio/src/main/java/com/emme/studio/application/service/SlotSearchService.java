package com.emme.studio.application.service;

import com.emme.studio.application.port.out.AppointmentCollisionPort;
import com.emme.studio.application.port.out.ArtistCapabilityRepository;
import com.emme.studio.application.port.out.OperatingHoursRepository;
import com.emme.studio.application.port.out.ServiceRepository;
import com.emme.studio.domain.model.DayOfWeek;
import com.emme.studio.domain.model.OperatingHours;
import com.emme.studio.domain.model.Service;
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

@org.springframework.stereotype.Service
@Transactional(readOnly = true)
public class SlotSearchService {

  private static final int SLOT_INTERVAL_MINUTES = 15;
  private static final ZoneId SALON_ZONE = ZoneId.of("America/Mexico_City");

  private final ArtistCapabilityRepository artistCapabilityRepository;
  private final OperatingHoursRepository operatingHoursRepository;
  private final ServiceRepository serviceRepository;
  private final AppointmentCollisionPort collisionPort;

  public SlotSearchService(
      ArtistCapabilityRepository artistCapabilityRepository,
      OperatingHoursRepository operatingHoursRepository,
      ServiceRepository serviceRepository,
      AppointmentCollisionPort collisionPort) {
    this.artistCapabilityRepository = artistCapabilityRepository;
    this.operatingHoursRepository = operatingHoursRepository;
    this.serviceRepository = serviceRepository;
    this.collisionPort = collisionPort;
  }

  public List<Slot> findAvailableSlots(UUID tenantId, UUID serviceId, LocalDate date) {
    Service service =
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
    List<Slot> availableSlots = new ArrayList<>();
    LocalTime slotStart = operatingHours.getOpensAt();
    LocalTime closesAt = operatingHours.getClosesAt();

    while (!slotStart.plusMinutes(service.getDurationMinutes()).isAfter(closesAt)) {
      Instant startsAt = toInstant(date, slotStart);
      Instant endsAt = toInstant(date, slotStart.plusMinutes(service.getDurationMinutes()));
      for (UUID artistId : artistIds) {
        if (!collisionPort.hasCollision(artistId, startsAt, endsAt)) {
          availableSlots.add(new Slot(artistId, startsAt, endsAt));
        }
      }
      slotStart = slotStart.plusMinutes(SLOT_INTERVAL_MINUTES);
    }
    return availableSlots;
  }

  private Optional<OperatingHours> findHours(UUID tenantId, LocalDate date) {
    return operatingHoursRepository.findByTenantIdAndDayOfWeek(
        tenantId, toSalonDayOfWeek(date.getDayOfWeek()));
  }

  private DayOfWeek toSalonDayOfWeek(java.time.DayOfWeek javaDay) {
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
    return ZonedDateTime.of(date, time, SALON_ZONE).toInstant();
  }

  public record Slot(UUID artistId, Instant startsAt, Instant endsAt) {}
}
