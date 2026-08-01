package com.emme.studio.application.service;

import com.emme.studio.adapter.out.persistence.entity.AppointmentEntity;
import com.emme.studio.adapter.out.persistence.repository.SpringDataAppointmentRepository;
import com.emme.studio.domain.model.AppointmentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CollisionDetector {

  private final SpringDataAppointmentRepository appointmentRepo;

  public CollisionDetector(SpringDataAppointmentRepository appointmentRepo) {
    this.appointmentRepo = appointmentRepo;
  }

  public boolean hasCollision(UUID artistId, Instant startsAt, Instant endsAt) {
    List<AppointmentEntity> overlapping =
        appointmentRepo.findByArtistIdAndStartsAtBetween(artistId, startsAt, endsAt);
    return overlapping.stream()
        .anyMatch(
            a ->
                a.getStatus() == AppointmentStatus.CONFIRMED
                    || a.getStatus() == AppointmentStatus.IN_PROGRESS);
  }
}
