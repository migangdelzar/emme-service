package com.emme.assistant.api.usecase;

import com.emme.assistant.api.command.AddConversationEventCommand;
import com.emme.assistant.api.result.ConversationEventInfo;

public interface AddConversationEventUseCase {
  ConversationEventInfo add(AddConversationEventCommand command);
}
