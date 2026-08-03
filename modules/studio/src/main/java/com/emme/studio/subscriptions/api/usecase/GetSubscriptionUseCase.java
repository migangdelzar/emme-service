package com.emme.studio.subscriptions.api.usecase;

import com.emme.studio.subscriptions.api.query.GetSubscriptionQuery;
import com.emme.studio.subscriptions.api.result.SubscriptionInfo;
import java.util.Optional;

public interface GetSubscriptionUseCase {
  Optional<SubscriptionInfo> get(GetSubscriptionQuery query);
}
