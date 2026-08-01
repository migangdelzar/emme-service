package com.emme.identity.api.result;

/** Result of customer authentication, including completion state for the profile. */
public record CustomerLoginResult(CustomerDetails customer, boolean needsPhone) {}
