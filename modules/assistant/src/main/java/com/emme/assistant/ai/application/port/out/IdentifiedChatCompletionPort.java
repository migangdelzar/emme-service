package com.emme.assistant.ai.application.port.out;

/** Chat completion boundary that reports the provider/model which produced the answer. */
public interface IdentifiedChatCompletionPort extends ChatCompletionPort {

  ChatCompletionResult completeWithIdentity(String conversationContext, String userMessage);

  @Override
  default String complete(String conversationContext, String userMessage) {
    return completeWithIdentity(conversationContext, userMessage).content();
  }

  record ChatCompletionResult(String content, String provider, String model) {
    public ChatCompletionResult {
      if (content == null || content.isBlank()) throw new IllegalArgumentException("content must not be blank");
      if (provider == null || provider.isBlank()) throw new IllegalArgumentException("provider must not be blank");
      if (model == null || model.isBlank()) throw new IllegalArgumentException("model must not be blank");
    }
  }
}
