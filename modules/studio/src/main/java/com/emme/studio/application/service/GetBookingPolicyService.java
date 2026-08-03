package com.emme.studio.application.service;

import com.emme.studio.api.result.BookingPolicyDetails;
import com.emme.studio.api.usecase.GetBookingPolicyUseCase;
import com.emme.studio.application.mapper.BusinessConfigurationApplicationMapper;
import com.emme.studio.application.port.out.BookingPolicyRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for retrieving booking policy. */
@Service
@Transactional(readOnly = true)
public class GetBookingPolicyService implements GetBookingPolicyUseCase {

  private final BookingPolicyRepository repository;

  public GetBookingPolicyService(BookingPolicyRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<BookingPolicyDetails> get(UUID tenantId) {
    return repository
        .findByTenantId(tenantId)
        .map(BusinessConfigurationApplicationMapper::toDetails);
  }
}
