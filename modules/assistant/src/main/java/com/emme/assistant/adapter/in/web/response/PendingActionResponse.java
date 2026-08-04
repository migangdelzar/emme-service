package com.emme.assistant.adapter.in.web.response;

import com.emme.assistant.api.result.PendingActionDetails;
import java.time.Instant;
import java.util.UUID;

public record PendingActionResponse(
    UUID id,
    UUID conversationId,
    String actionType,
    String status,
    String details,
    Instant expiresAt,
    Instant createdAt) {
  public static PendingActionResponse from(PendingActionDetails info) {
    return new PendingActionResponse(
        info.id(),
        info.conversationId(),
        info.actionType().name(),
        info.status().name(),
        info.details(),
        info.expiresAt(),
        info.createdAt());
  }
}
