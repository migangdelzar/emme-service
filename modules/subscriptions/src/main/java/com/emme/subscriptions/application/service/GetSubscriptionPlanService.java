package com.emme.subscriptions.application.service;

import com.emme.subscriptions.api.query.GetSubscriptionPlanQuery;
import com.emme.subscriptions.api.type.PlanType;
import com.emme.subscriptions.api.usecase.GetSubscriptionPlanUseCase;
import com.emme.subscriptions.application.port.out.SubscriptionRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetSubscriptionPlanService implements GetSubscriptionPlanUseCase {
  private final SubscriptionRepository repository;

  @Override
  public Optional<PlanType> getPlan(GetSubscriptionPlanQuery query) {
    return repository.find().map(subscription -> subscription.plan());
  }
}
