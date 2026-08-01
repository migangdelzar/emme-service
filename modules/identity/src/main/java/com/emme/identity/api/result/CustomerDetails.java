package com.emme.identity.api.result;

import java.util.UUID;

/** Stable customer identity data returned by Identity use cases. */
public record CustomerDetails(UUID id, String email, String name, String phone, String provider) {}
