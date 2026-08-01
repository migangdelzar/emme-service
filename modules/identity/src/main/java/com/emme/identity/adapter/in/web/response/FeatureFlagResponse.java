package com.emme.identity.adapter.in.web.response;

import com.emme.studio.subscriptions.api.PlanType;
import java.util.UUID;

/** HTTP representation of a platform or tenant feature flag. */
public record FeatureFlagResponse(
    UUID id, String code, boolean enabled, PlanType planRequired, String description) {}
