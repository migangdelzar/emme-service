package com.emme.assistant.ai.application.port.out;

/**
 * Compatibility boundary retained while chat callers migrate to the canonical policy capability.
 *
 * @deprecated migrate callers to {@code com.emme.ai.contracts.model.AiChatCompletion}
 */
@Deprecated
public interface ChatCompletionPort {

  String complete(String conversationContext, String userMessage);
}
