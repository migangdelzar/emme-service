package com.emme.assistant.adapter.in.web.request;

import com.emme.kernel.type.ChannelType;
import java.util.UUID;

public record StartConversationRequest(UUID participantId, ChannelType channel) {}
