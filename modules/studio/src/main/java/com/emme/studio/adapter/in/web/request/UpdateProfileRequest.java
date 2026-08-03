package com.emme.studio.adapter.in.web.request;

/** HTTP request for updating business profile settings. */
public record UpdateProfileRequest(String displayName, String timeZone, String locale) {}
