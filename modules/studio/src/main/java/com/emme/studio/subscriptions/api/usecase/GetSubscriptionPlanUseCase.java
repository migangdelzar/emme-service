package com.emme.studio.subscriptions.api.usecase;

import com.emme.studio.subscriptions.api.query.GetSubscriptionPlanQuery;
import com.emme.studio.subscriptions.api.type.PlanType;
import java.util.Optional;

public interface GetSubscriptionPlanUseCase {
  Optional<PlanType> getPlan(GetSubscriptionPlanQuery query);
}
