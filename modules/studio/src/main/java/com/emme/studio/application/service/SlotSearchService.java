package com.emme.studio.application.service;

import com.emme.studio.entity.ArtistCapability;
import com.emme.studio.entity.ArtistCapabilityRepository;
import com.emme.studio.entity.BookingPolicyRepository;
import com.emme.studio.entity.DayOfWeek;
import com.emme.studio.entity.OperatingHours;
import com.emme.studio.entity.OperatingHoursRepository;
import com.emme.studio.entity.Service;
import com.emme.studio.entity.ServiceRepository;
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

  private final ArtistCapabilityRepository artistCapabilityRepo;
  private final OperatingHoursRepository operatingHoursRepo;
  private final BookingPolicyRepository bookingPolicyRepo;
  private final ServiceRepository serviceRepo;
  private final CollisionDetector collisionDetector;

  public SlotSearchService(
      ArtistCapabilityRepository artistCapabilityRepo,
      OperatingHoursRepository operatingHoursRepo,
      BookingPolicyRepository bookingPolicyRepo,
      ServiceRepository serviceRepo,
      CollisionDetector collisionDetector) {
    this.artistCapabilityRepo = artistCapabilityRepo;
    this.operatingHoursRepo = operatingHoursRepo;
    this.bookingPolicyRepo = bookingPolicyRepo;
    this.serviceRepo = serviceRepo;
    this.collisionDetector = collisionDetector;
  }

  public List<Slot> findAvailableSlots(UUID tenantId, UUID serviceId, LocalDate date) {
    Service service =
        serviceRepo
            .findById(serviceId)
            .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));
    int durationMinutes = service.getDurationMinutes();

    DayOfWeek salonDay = toSalonDayOfWeek(date.getDayOfWeek());
    Optional<OperatingHours> hours =
        operatingHoursRepo.findByTenantIdAndDayOfWeek(tenantId, salonDay);
    if (hours.isEmpty() || !hours.get().isActive()) {
      return List.of();
    }

    OperatingHours operatingHours = hours.get();
    ZoneId zone = ZoneId.of("America/Mexico_City");

    List<ArtistCapability> capabilities =
        artistCapabilityRepo.findByServiceIdAndActiveTrue(serviceId);
    List<UUID> artistIds = capabilities.stream().map(c -> c.getArtist().getId()).toList();

    List<Slot> availableSlots = new ArrayList<>();
    LocalTime slotStart = operatingHours.getOpensAt();
    LocalTime closesAt = operatingHours.getClosesAt();

    while (!slotStart.plusMinutes(durationMinutes).isAfter(closesAt)) {
      Instant startsAt = toInstant(date, slotStart, zone);
      Instant endsAt = toInstant(date, slotStart.plusMinutes(durationMinutes), zone);

      for (UUID artistId : artistIds) {
        if (!collisionDetector.hasCollision(artistId, startsAt, endsAt)) {
          availableSlots.add(new Slot(artistId, startsAt, endsAt));
        }
      }

      slotStart = slotStart.plusMinutes(SLOT_INTERVAL_MINUTES);
    }

    return availableSlots;
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

  private Instant toInstant(LocalDate date, LocalTime time, ZoneId zone) {
    return ZonedDateTime.of(date, time, zone).toInstant();
  }

  public record Slot(UUID artistId, Instant startsAt, Instant endsAt) {}
}
