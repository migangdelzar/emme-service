package com.emme.assistant.api.result;

import com.emme.assistant.api.type.ConversationStatusView;
import com.emme.kernel.type.ChannelType;
import java.time.Instant;
import java.util.UUID;

public record ConversationInfo(
    UUID id,
    UUID tenantId,
    UUID participantId,
    ChannelType channel,
    ConversationStatusView status,
    Instant startedAt) {}
