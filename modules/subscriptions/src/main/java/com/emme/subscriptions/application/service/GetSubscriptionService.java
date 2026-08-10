package com.emme.subscriptions.application.service;

import com.emme.subscriptions.api.query.GetSubscriptionQuery;
import com.emme.subscriptions.api.result.SubscriptionDetails;
import com.emme.subscriptions.api.usecase.GetSubscriptionUseCase;
import com.emme.subscriptions.application.mapper.SubscriptionApplicationMapper;
import com.emme.subscriptions.application.port.out.SubscriptionRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetSubscriptionService implements GetSubscriptionUseCase {
  private final SubscriptionRepository repository;

  public GetSubscriptionService(SubscriptionRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<SubscriptionDetails> get(GetSubscriptionQuery query) {
    return repository.findByTenantId(query.tenantId()).map(SubscriptionApplicationMapper::toResult);
  }
}
