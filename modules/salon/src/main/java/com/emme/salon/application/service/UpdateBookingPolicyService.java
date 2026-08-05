package com.emme.salon.application.service;

import com.emme.salon.api.result.BookingPolicyDetails;
import com.emme.salon.api.usecase.UpdateBookingPolicyUseCase;
import com.emme.salon.application.mapper.BusinessConfigurationApplicationMapper;
import com.emme.salon.application.port.out.BookingPolicyRepository;
import com.emme.salon.domain.model.BookingPolicy;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for updating booking policy. */
@Service
@Transactional
public class UpdateBookingPolicyService implements UpdateBookingPolicyUseCase {

  private final BookingPolicyRepository repository;

  public UpdateBookingPolicyService(BookingPolicyRepository repository) {
    this.repository = repository;
  }

  @Override
  public BookingPolicyDetails update(
      UUID tenantId, int minNotice, int maxAdvance, int cancelWindow, boolean allowOverlap) {
    BookingPolicy policy =
        repository
            .findByTenantId(tenantId)
            .orElse(new BookingPolicy(tenantId, minNotice, maxAdvance, cancelWindow, allowOverlap));
    policy.update(minNotice, maxAdvance, cancelWindow, allowOverlap);
    return BusinessConfigurationApplicationMapper.toDetails(repository.save(policy));
  }
}
