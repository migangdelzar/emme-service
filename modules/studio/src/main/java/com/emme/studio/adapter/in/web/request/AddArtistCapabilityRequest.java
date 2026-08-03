package com.emme.studio.adapter.in.web.request;

import java.util.UUID;

/** HTTP request for assigning a service capability to an artist. */
public record AddArtistCapabilityRequest(UUID serviceId) {}
