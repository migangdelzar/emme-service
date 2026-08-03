package com.emme.studio.application.service;

import com.emme.studio.api.result.OperatingHoursDetails;
import com.emme.studio.api.type.BusinessDay;
import com.emme.studio.api.usecase.UpdateOperatingHoursUseCase;
import com.emme.studio.application.mapper.BusinessConfigurationApplicationMapper;
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
  public OperatingHoursDetails update(
      UUID tenantId, BusinessDay day, LocalTime opensAt, LocalTime closesAt, boolean active) {
    DayOfWeek domainDay = DayOfWeek.valueOf(day.name());
    OperatingHours hours =
        repository
            .findByTenantIdAndDayOfWeek(tenantId, domainDay)
            .orElse(new OperatingHours(tenantId, domainDay, opensAt, closesAt));
    hours.update(opensAt, closesAt, active);
    return BusinessConfigurationApplicationMapper.toDetails(repository.save(hours));
  }
}
