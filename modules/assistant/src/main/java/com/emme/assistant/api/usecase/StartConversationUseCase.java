package com.emme.assistant.api.usecase;

import com.emme.assistant.api.command.StartConversationCommand;
import com.emme.assistant.api.result.ConversationInfo;

public interface StartConversationUseCase {
  ConversationInfo start(StartConversationCommand command);
}
