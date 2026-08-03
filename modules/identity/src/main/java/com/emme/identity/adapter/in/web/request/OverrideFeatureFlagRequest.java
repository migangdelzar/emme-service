package com.emme.identity.adapter.in.web.request;

/** Input used to override a feature flag for the current tenant. */
public record OverrideFeatureFlagRequest(boolean enabled) {}
