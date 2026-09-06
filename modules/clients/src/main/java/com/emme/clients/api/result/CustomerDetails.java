package com.emme.clients.api.result;

import com.emme.clients.domain.model.CustomerStatus;
import java.util.UUID;

/** Stable public customer representation returned by Studio use cases. */
public record CustomerDetails(
    UUID id, String name, String phone, String email, CustomerStatus status) {}
