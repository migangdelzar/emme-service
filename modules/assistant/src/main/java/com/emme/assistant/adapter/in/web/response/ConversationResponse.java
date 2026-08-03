package com.emme.assistant.adapter.in.web.response;

import com.emme.assistant.api.result.ConversationInfo;
import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
    UUID id, UUID tenantId, UUID participantId, String channel, String status, Instant startedAt) {
  public static ConversationResponse from(ConversationInfo info) {
    return new ConversationResponse(
        info.id(),
        info.tenantId(),
        info.participantId(),
        info.channel().name(),
        info.status().name(),
        info.startedAt());
  }
}
