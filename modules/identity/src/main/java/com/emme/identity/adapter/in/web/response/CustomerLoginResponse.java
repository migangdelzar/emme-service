package com.emme.identity.adapter.in.web.response;

/** HTTP representation of customer authentication and profile completion state. */
public record CustomerLoginResponse(boolean needsPhone, CustomerProfileResponse customer) {}
