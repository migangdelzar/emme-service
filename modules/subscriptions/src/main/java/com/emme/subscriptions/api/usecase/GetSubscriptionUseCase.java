package com.emme.subscriptions.api.usecase;

import com.emme.subscriptions.api.query.GetSubscriptionQuery;
import com.emme.subscriptions.api.result.SubscriptionDetails;
import java.util.Optional;

public interface GetSubscriptionUseCase {
  Optional<SubscriptionDetails> get(GetSubscriptionQuery query);
}
