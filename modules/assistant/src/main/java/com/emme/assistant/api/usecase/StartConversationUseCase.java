package com.emme.assistant.api.usecase;

import com.emme.assistant.domain.model.Conversation;
import com.emme.kernel.type.ChannelType;
import java.util.UUID;

public interface StartConversationUseCase {
  Conversation start(UUID tenantId, UUID participantId, ChannelType channel);
}
