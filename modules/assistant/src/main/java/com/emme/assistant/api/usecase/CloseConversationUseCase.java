package com.emme.assistant.api.usecase;

import com.emme.assistant.api.command.CloseConversationCommand;
import com.emme.assistant.api.result.ConversationDetails;

public interface CloseConversationUseCase {
  ConversationDetails close(CloseConversationCommand command);
}
