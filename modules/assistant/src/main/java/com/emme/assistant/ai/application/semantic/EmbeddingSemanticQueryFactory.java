package com.emme.assistant.ai.application.semantic;

import com.emme.ai.contracts.embedding.EmbeddingService;
import com.emme.kernel.context.AiExecutionContext;
import java.util.Objects;

/** Builds a semantic query by embedding the text exactly once at the operation boundary. */
public final class EmbeddingSemanticQueryFactory implements SemanticQueryFactory {

  private final EmbeddingService embeddings;

  public EmbeddingSemanticQueryFactory(EmbeddingService embeddings) {
    this.embeddings = Objects.requireNonNull(embeddings, "embeddings must not be null");
  }

  @Override
  public SemanticQuery create(String rawText, AiExecutionContext context) {
    Objects.requireNonNull(context, "context must not be null");
    if (rawText == null || rawText.isBlank()) {
      throw new IllegalArgumentException("rawText must not be blank");
    }
    return new SemanticQuery(rawText, embeddings.embed(rawText));
  }
}
