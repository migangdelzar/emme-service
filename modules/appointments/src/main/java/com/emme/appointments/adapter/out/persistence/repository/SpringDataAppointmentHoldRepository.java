package com.emme.appointments.adapter.out.persistence.repository;

import com.emme.appointments.adapter.out.persistence.entity.AppointmentHoldEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataAppointmentHoldRepository
    extends JpaRepository<AppointmentHoldEntity, UUID> {

  Optional<AppointmentHoldEntity> findByIdempotencyKey(String idempotencyKey);
}
