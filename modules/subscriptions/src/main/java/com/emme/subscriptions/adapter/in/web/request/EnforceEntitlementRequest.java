package com.emme.subscriptions.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;

public record EnforceEntitlementRequest(@NotBlank String entitlement) {}
