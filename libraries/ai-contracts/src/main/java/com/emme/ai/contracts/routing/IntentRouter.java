package com.emme.ai.contracts.routing;

import com.emme.ai.contracts.context.AiExecutionContext;

/** Port for deterministic, semantic, and abstaining intent routing. */
public interface IntentRouter {

  IntentRoute route(RouteRequest request, AiExecutionContext context);
}
