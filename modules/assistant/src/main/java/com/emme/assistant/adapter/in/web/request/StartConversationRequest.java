package com.emme.assistant.adapter.in.web.request;

import com.emme.kernel.type.ChannelType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StartConversationRequest(
    @NotNull UUID participantId, @NotNull ChannelType channel) {}
