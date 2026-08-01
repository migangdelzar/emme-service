package com.emme.identity.adapter.in.web.request;

import com.emme.studio.subscriptions.api.PlanType;

/** Input used to create or initialize a platform feature flag. */
public record CreateFeatureFlagRequest(String code, boolean enabled, PlanType planRequired) {}
