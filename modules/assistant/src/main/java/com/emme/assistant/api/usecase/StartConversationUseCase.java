package com.emme.assistant.api.usecase;

import com.emme.assistant.api.command.StartConversationCommand;
import com.emme.assistant.api.result.ConversationDetails;

public interface StartConversationUseCase {
  ConversationDetails start(StartConversationCommand command);
}
