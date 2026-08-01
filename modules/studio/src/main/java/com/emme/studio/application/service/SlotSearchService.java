package com.emme.studio.application.service;

import com.emme.studio.adapter.out.persistence.entity.ArtistCapabilityEntity;
import com.emme.studio.adapter.out.persistence.entity.OperatingHoursEntity;
import com.emme.studio.adapter.out.persistence.entity.ServiceEntity;
import com.emme.studio.adapter.out.persistence.repository.SpringDataArtistCapabilityRepository;
import com.emme.studio.adapter.out.persistence.repository.SpringDataBookingPolicyRepository;
import com.emme.studio.adapter.out.persistence.repository.SpringDataOperatingHoursRepository;
import com.emme.studio.adapter.out.persistence.repository.SpringDataServiceRepository;
import com.emme.studio.domain.model.DayOfWeek;
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

  private final SpringDataArtistCapabilityRepository artistCapabilityRepo;
  private final SpringDataOperatingHoursRepository operatingHoursRepo;
  private final SpringDataBookingPolicyRepository bookingPolicyRepo;
  private final SpringDataServiceRepository serviceRepo;
  private final CollisionDetector collisionDetector;

  public SlotSearchService(
      SpringDataArtistCapabilityRepository artistCapabilityRepo,
      SpringDataOperatingHoursRepository operatingHoursRepo,
      SpringDataBookingPolicyRepository bookingPolicyRepo,
      SpringDataServiceRepository serviceRepo,
      CollisionDetector collisionDetector) {
    this.artistCapabilityRepo = artistCapabilityRepo;
    this.operatingHoursRepo = operatingHoursRepo;
    this.bookingPolicyRepo = bookingPolicyRepo;
    this.serviceRepo = serviceRepo;
    this.collisionDetector = collisionDetector;
  }

  public List<Slot> findAvailableSlots(UUID tenantId, UUID serviceId, LocalDate date) {
    ServiceEntity service =
        serviceRepo
            .findById(serviceId)
            .orElseThrow(
                () -> new IllegalArgumentException("ServiceEntity not found: " + serviceId));
    int durationMinutes = service.getDurationMinutes();

    DayOfWeek salonDay = toSalonDayOfWeek(date.getDayOfWeek());
    Optional<OperatingHoursEntity> hours =
        operatingHoursRepo.findByTenantIdAndDayOfWeek(tenantId, salonDay);
    if (hours.isEmpty() || !hours.get().isActive()) {
      return List.of();
    }

    OperatingHoursEntity operatingHours = hours.get();
    ZoneId zone = ZoneId.of("America/Mexico_City");

    List<ArtistCapabilityEntity> capabilities =
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
