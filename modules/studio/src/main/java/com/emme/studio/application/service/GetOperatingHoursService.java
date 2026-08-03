package com.emme.studio.application.service;

import com.emme.studio.api.result.OperatingHoursDetails;
import com.emme.studio.api.usecase.GetOperatingHoursUseCase;
import com.emme.studio.application.mapper.BusinessConfigurationApplicationMapper;
import com.emme.studio.application.port.out.OperatingHoursRepository;
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
  public List<OperatingHoursDetails> get(UUID tenantId) {
    return repository.findByTenantId(tenantId).stream()
        .map(BusinessConfigurationApplicationMapper::toDetails)
        .toList();
  }
}
