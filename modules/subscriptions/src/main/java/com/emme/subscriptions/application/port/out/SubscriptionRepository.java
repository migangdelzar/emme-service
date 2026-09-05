package com.emme.subscriptions.application.port.out;

import com.emme.subscriptions.domain.model.Subscription;
import java.util.Optional;

public interface SubscriptionRepository {
  Optional<Subscription> find();

  Subscription save(Subscription subscription);
}
