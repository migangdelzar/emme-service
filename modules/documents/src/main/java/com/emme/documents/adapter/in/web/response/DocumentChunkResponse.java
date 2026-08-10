package com.emme.documents.adapter.in.web.response;

import java.util.UUID;

/** HTTP representation of a document chunk. */
public record DocumentChunkResponse(
    UUID id, UUID documentId, int chunkIndex, String content, String contentFingerprint) {}
