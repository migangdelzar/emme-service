package com.emme.assistant.api.result;

import com.emme.assistant.domain.model.ConversationStatus;
import com.emme.kernel.type.ChannelType;
import java.time.Instant;
import java.util.UUID;

public record ConversationInfo(
    UUID id,
    UUID tenantId,
    UUID participantId,
    ChannelType channel,
    ConversationStatus status,
    Instant startedAt) {}
