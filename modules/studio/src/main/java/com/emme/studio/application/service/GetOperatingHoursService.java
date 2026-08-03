package com.emme.studio.application.service;

import com.emme.studio.api.usecase.GetOperatingHoursUseCase;
import com.emme.studio.application.port.out.OperatingHoursRepository;
import com.emme.studio.domain.model.OperatingHours;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for retrieving operating hours. */
@Service
@Transactional(readOnly = true)
public class GetOperatingHoursService implements GetOperatingHoursUseCase {

  private final OperatingHoursRepository repository;

  public GetOperatingHoursService(OperatingHoursRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<OperatingHours> get(UUID tenantId) {
    return repository.findByTenantId(tenantId);
  }
}
