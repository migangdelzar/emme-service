package com.emme.studio.api.result;

import java.util.UUID;

/** Stable public business-profile representation. */
public record BusinessProfileDetails(UUID id, String displayName, String timeZone, String locale) {}
