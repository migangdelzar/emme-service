package com.emme.salon.adapter.in.web.request;

/** HTTP request for updating business profile settings. */
public record UpdateProfileRequest(String displayName, String timeZone, String locale) {}
