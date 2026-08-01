package com.emme.studio.documents.adapter.in.web.response;

import java.util.UUID;

/** HTTP representation of a document, independent of JPA. */
public record DocumentResponse(
    UUID id, UUID tenantId, String name, String sourceType, String status, int version) {}
