package com.emme.studio.subscriptions.api.usecase;

import com.emme.studio.subscriptions.api.query.GetSubscriptionQuery;
import com.emme.studio.subscriptions.api.result.SubscriptionDetails;
import java.util.Optional;

public interface GetSubscriptionUseCase {
  Optional<SubscriptionDetails> get(GetSubscriptionQuery query);
}
