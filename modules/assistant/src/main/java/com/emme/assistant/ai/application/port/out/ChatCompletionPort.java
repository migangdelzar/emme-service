package com.emme.assistant.ai.application.port.out;

/** Provider-neutral boundary for non-structured chat completion. */
public interface ChatCompletionPort {

  String complete(String conversationContext, String userMessage);
}
