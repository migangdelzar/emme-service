package com.emme.salon.application.service;

import com.emme.salon.api.result.BusinessProfileDetails;
import com.emme.salon.api.usecase.GetBusinessProfileConfigUseCase;
import com.emme.salon.application.mapper.BusinessConfigurationApplicationMapper;
import com.emme.salon.application.port.out.BusinessProfileRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for retrieving the editable business profile. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetBusinessProfileConfigService implements GetBusinessProfileConfigUseCase {

  private final BusinessProfileRepository repository;

  @Override
  public Optional<BusinessProfileDetails> get(UUID tenantId) {
    return repository.find().map(BusinessConfigurationApplicationMapper::toDetails);
  }
}
