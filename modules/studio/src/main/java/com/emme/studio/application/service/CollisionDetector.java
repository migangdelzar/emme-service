package com.emme.studio.application.service;

import com.emme.studio.entity.Appointment;
import com.emme.studio.entity.AppointmentRepository;
import com.emme.studio.entity.AppointmentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CollisionDetector {

  private final AppointmentRepository appointmentRepo;

  public CollisionDetector(AppointmentRepository appointmentRepo) {
    this.appointmentRepo = appointmentRepo;
  }

  public boolean hasCollision(UUID artistId, Instant startsAt, Instant endsAt) {
    List<Appointment> overlapping =
        appointmentRepo.findByArtistIdAndStartsAtBetween(artistId, startsAt, endsAt);
    return overlapping.stream()
        .anyMatch(
            a ->
                a.getStatus() == AppointmentStatus.CONFIRMED
                    || a.getStatus() == AppointmentStatus.IN_PROGRESS);
  }
}
