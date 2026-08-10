package com.emme.subscriptions.api.usecase;

import com.emme.subscriptions.api.query.GetSubscriptionPlanQuery;
import com.emme.subscriptions.api.type.PlanType;
import java.util.Optional;

public interface GetSubscriptionPlanUseCase {
  Optional<PlanType> getPlan(GetSubscriptionPlanQuery query);
}
