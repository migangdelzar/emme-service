package com.emme.assistant.ai.application.port.out;

import com.emme.ai.contracts.model.ChatModel;

/** Provider-neutral boundary for non-structured chat completion. */
public interface ChatCompletionPort extends ChatModel {

  String complete(String conversationContext, String userMessage);
}
