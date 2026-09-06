package com.emme.documents.adapter.in.web.response;

import com.emme.documents.api.type.DocumentStatus;
import java.util.UUID;

/** HTTP representation of a document, independent of JPA. */
public record DocumentResponse(
    UUID id, UUID tenantId, String name, String sourceType, DocumentStatus status, int version) {}
