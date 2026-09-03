package com.emme.ai.platform.adapter.out.provider.springai;

import com.emme.ai.contracts.model.AiModelProvider;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Provider-neutral model contract backed by Spring AI chat and embedding adapters. */
public final class SpringAiModelProvider implements AiModelProvider {

  private final SpringAiChatModel chatModel;
  private final Optional<SpringAiEmbeddingModel> embeddingModel;
  private final Optional<SpringAiVisionModel> visionModel;

  public SpringAiModelProvider(
      SpringAiChatModel chatModel, Optional<SpringAiEmbeddingModel> embeddingModel) {
    this(chatModel, embeddingModel, Optional.empty());
  }

  public SpringAiModelProvider(
      SpringAiChatModel chatModel,
      Optional<SpringAiEmbeddingModel> embeddingModel,
      Optional<SpringAiVisionModel> visionModel) {
    this.chatModel = Objects.requireNonNull(chatModel, "chatModel must not be null");
    this.embeddingModel = Objects.requireNonNull(embeddingModel, "embeddingModel must not be null");
    this.visionModel = Objects.requireNonNull(visionModel, "visionModel must not be null");
  }

  @Override
  public String name() {
    return chatModel.provider();
  }

  @Override
  public String chat(String conversationContext, String userMessage) {
    return chatModel.complete(conversationContext, userMessage);
  }

  @Override
  public List<Float> embed(String text) {
    return embeddingModel.map(model -> model.embed(text)).orElseGet(List::of);
  }

  @Override
  public String caption(String imageBase64) {
    return visionModel
        .map(model -> model.caption(imageBase64))
        .orElseThrow(
            () ->
                new UnsupportedOperationException(
                    "Provider '" + name() + "' does not support vision captioning"));
  }
}
