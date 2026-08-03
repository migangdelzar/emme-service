package com.emme.studio.subscriptions.application.service;

import com.emme.studio.subscriptions.api.command.CreateSubscriptionCommand;
import com.emme.studio.subscriptions.api.result.SubscriptionInfo;
import com.emme.studio.subscriptions.api.usecase.CreateSubscriptionUseCase;
import com.emme.studio.subscriptions.application.mapper.SubscriptionApplicationMapper;
import com.emme.studio.subscriptions.application.port.out.SubscriptionRepository;
import com.emme.studio.subscriptions.domain.model.Subscription;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateSubscriptionService implements CreateSubscriptionUseCase {
  private final SubscriptionRepository repository;

  public CreateSubscriptionService(SubscriptionRepository repository) {
    this.repository = repository;
  }

  @Override
  public SubscriptionInfo create(CreateSubscriptionCommand command) {
    if (repository.findByTenantId(command.tenantId()).isPresent()) {
      throw new IllegalArgumentException(
          "Subscription already exists for tenant: " + command.tenantId());
    }
    Subscription subscription =
        new Subscription(
            command.tenantId(), command.plan(), Instant.now().plusSeconds(365L * 24 * 3600));
    return SubscriptionApplicationMapper.toInfo(repository.save(subscription));
  }
}
