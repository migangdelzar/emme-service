package com.emme.salon.adapter.out.persistence.adapter;

import com.emme.salon.adapter.out.persistence.entity.OperatingHoursEntity;
import com.emme.salon.adapter.out.persistence.mapper.OperatingHoursPersistenceMapper;
import com.emme.salon.adapter.out.persistence.repository.SpringDataOperatingHoursRepository;
import com.emme.salon.application.port.out.OperatingHoursRepository;
import com.emme.salon.domain.model.DayOfWeek;
import com.emme.salon.domain.model.OperatingHours;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Implements the operating-hours port using Spring Data JPA. */
@Component
public class OperatingHoursPersistenceAdapter implements OperatingHoursRepository {

  private final SpringDataOperatingHoursRepository repository;
  private final OperatingHoursPersistenceMapper mapper;

  public OperatingHoursPersistenceAdapter(SpringDataOperatingHoursRepository repository) {
    this.repository = repository;
    this.mapper = new OperatingHoursPersistenceMapper();
  }

  @Override
  public OperatingHours save(OperatingHours operatingHours) {
    OperatingHoursEntity entity =
        operatingHours.getId() == null
            ? mapper.toNewEntity(operatingHours)
            : repository
                .findByTenantIdAndId(operatingHours.getTenantId(), operatingHours.getId())
                .orElseThrow();
    mapper.updateEntity(operatingHours, entity);
    return mapper.toDomain(repository.save(entity));
  }

  @Override
  public Optional<OperatingHours> findByTenantIdAndDayOfWeek(UUID tenantId, DayOfWeek dayOfWeek) {
    return repository.findByTenantIdAndDayOfWeek(tenantId, dayOfWeek).map(mapper::toDomain);
  }

  @Override
  public List<OperatingHours> findByTenantId(UUID tenantId) {
    return repository.findByTenantId(tenantId).stream().map(mapper::toDomain).toList();
  }
}
