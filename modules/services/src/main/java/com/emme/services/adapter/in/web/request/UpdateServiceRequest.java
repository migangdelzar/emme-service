package com.emme.services.adapter.in.web.request;

import java.math.BigDecimal;

/** HTTP request for updating a service catalog entry. */
public record UpdateServiceRequest(
    String name, String category, String description, int durationMinutes, BigDecimal basePrice) {}
