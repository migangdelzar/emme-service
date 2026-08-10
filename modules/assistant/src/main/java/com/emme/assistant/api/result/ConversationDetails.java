package com.emme.assistant.api.result;

import com.emme.assistant.api.type.ConversationStatus;
import com.emme.kernel.type.ChannelType;
import java.time.Instant;
import java.util.UUID;

public record ConversationDetails(
    UUID id,
    UUID tenantId,
    UUID participantId,
    ChannelType channel,
    ConversationStatus status,
    Instant startedAt) {}
