package com.emme.salon.application.service;

import com.emme.salon.api.result.OperatingHoursDetails;
import com.emme.salon.api.usecase.GetOperatingHoursUseCase;
import com.emme.salon.application.mapper.BusinessConfigurationApplicationMapper;
import com.emme.salon.application.port.out.OperatingHoursRepository;
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
    return repository.findAll().stream()
        .map(BusinessConfigurationApplicationMapper::toDetails)
        .toList();
  }
}
