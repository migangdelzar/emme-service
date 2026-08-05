package com.emme.salon.api.result;

import java.util.UUID;

/** Stable public business-profile representation. */
public record BusinessProfileDetails(UUID id, String displayName, String timeZone, String locale) {}
