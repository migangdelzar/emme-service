package com.emme.ai.platform.adapter.out.capability;

import com.emme.ai.contracts.embedding.EmbeddingService;
import com.emme.ai.contracts.model.AiModelProvider;
import java.util.List;
import org.springframework.stereotype.Service;

/** Adapts the provider-neutral embedding capability to the configured model provider. */
@Service
public class AiEmbeddingAdapter implements EmbeddingService {

  private final AiModelProvider provider;

  public AiEmbeddingAdapter(AiModelProvider provider) {
    this.provider = provider;
  }

  @Override
  public List<Float> embed(String text) {
    return provider.embed(text);
  }
}
