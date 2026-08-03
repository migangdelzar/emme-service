package com.emme.assistant.api.usecase;

import com.emme.assistant.api.command.CloseConversationCommand;
import com.emme.assistant.api.result.ConversationInfo;

public interface CloseConversationUseCase {
  ConversationInfo close(CloseConversationCommand command);
}
