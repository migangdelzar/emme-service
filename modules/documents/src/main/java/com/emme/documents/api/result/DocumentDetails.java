package com.emme.documents.api.result;

import com.emme.documents.api.type.DocumentStatus;
import java.util.UUID;

/** Stable public representation of a document. */
public record DocumentDetails(
    UUID id, UUID tenantId, String name, String sourceType, DocumentStatus status, int version) {}
