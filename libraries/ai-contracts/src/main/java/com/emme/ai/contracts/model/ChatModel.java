package com.emme.ai.contracts.model;

/**
 * Provider-mechanics compatibility contract retained for existing Spring AI adapter wiring.
 *
 * @deprecated use {@link AiChatCompletion} at policy-facing application boundaries
 */
@Deprecated
public interface ChatModel {

  /** Completes a prepared conversation request. */
  String complete(String conversationContext, String userMessage);
}
