package com.emme.studio.documents.api.result;

import java.util.UUID;

/** Stable public representation of a document chunk. */
public record DocumentChunkDetails(
    UUID id, UUID documentId, int chunkIndex, String content, String contentFingerprint) {}
