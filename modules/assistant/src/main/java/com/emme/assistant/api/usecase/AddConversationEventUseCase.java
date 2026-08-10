package com.emme.assistant.api.usecase;

import com.emme.assistant.api.command.AddConversationEventCommand;
import com.emme.assistant.api.result.ConversationEventDetails;

public interface AddConversationEventUseCase {
  ConversationEventDetails add(AddConversationEventCommand command);
}
