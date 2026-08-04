package com.emme.studio.documents.api.result;

import java.util.UUID;

/** Stable public representation of a document. */
public record DocumentDetails(
    UUID id, UUID tenantId, String name, String sourceType, String status, int version) {}
