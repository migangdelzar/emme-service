package com.emme.identity.api.result;

import com.emme.studio.subscriptions.api.type.PlanType;
import java.util.UUID;

/** Public feature-flag representation returned by Identity use cases. */
public record FeatureFlagInfo(
    UUID id, String code, boolean enabled, PlanType planRequired, String description) {}
