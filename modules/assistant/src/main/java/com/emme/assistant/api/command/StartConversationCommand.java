package com.emme.assistant.api.command;

import com.emme.kernel.type.ChannelType;
import java.util.UUID;

public record StartConversationCommand(UUID tenantId, UUID participantId, ChannelType channel) {}
