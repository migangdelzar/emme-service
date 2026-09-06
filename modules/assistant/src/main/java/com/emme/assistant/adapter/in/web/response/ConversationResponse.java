package com.emme.assistant.adapter.in.web.response;

import com.emme.assistant.api.result.ConversationDetails;
import com.emme.assistant.api.type.ConversationStatus;
import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
    UUID id,
    UUID tenantId,
    UUID participantId,
    String channel,
    ConversationStatus status,
    Instant startedAt) {
  public static ConversationResponse from(ConversationDetails info) {
    return new ConversationResponse(
        info.id(),
        info.tenantId(),
        info.participantId(),
        info.channel().name(),
        info.status(),
        info.startedAt());
  }
}
