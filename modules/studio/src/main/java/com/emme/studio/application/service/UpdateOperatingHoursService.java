package com.emme.studio.application.service;

import com.emme.studio.api.usecase.UpdateOperatingHoursUseCase;
import com.emme.studio.application.port.out.OperatingHoursRepository;
import com.emme.studio.domain.model.DayOfWeek;
import com.emme.studio.domain.model.OperatingHours;
import java.time.LocalTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for updating operating hours. */
@Service
@Transactional
public class UpdateOperatingHoursService implements UpdateOperatingHoursUseCase {

  private final OperatingHoursRepository repository;

  public UpdateOperatingHoursService(OperatingHoursRepository repository) {
    this.repository = repository;
  }

  @Override
  public OperatingHours update(
      UUID tenantId, DayOfWeek day, LocalTime opensAt, LocalTime closesAt, boolean active) {
    OperatingHours hours =
        repository
            .findByTenantIdAndDayOfWeek(tenantId, day)
            .orElse(new OperatingHours(tenantId, day, opensAt, closesAt));
    hours.update(opensAt, closesAt, active);
    return repository.save(hours);
  }
}
