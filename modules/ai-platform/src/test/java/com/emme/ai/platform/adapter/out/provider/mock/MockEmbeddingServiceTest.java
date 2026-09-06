package com.emme.ai.platform.adapter.out.provider.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.ai.platform.configuration.AiProviderProperties;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MockEmbeddingServiceTest {

  private final MockEmbeddingService embeddings =
      new MockEmbeddingService(new AiProviderProperties("mock", null, null, false));

  @Test
  void returnsAConfiguredVersionedVectorInsideAnAiExecutionContext() {
    var context = context();

    var vector = AiExecutionContextScope.call(context, () -> embeddings.embed("gel manicure"));

    assertThat(vector.values()).hasSize(AiProviderProperties.DEFAULT_EMBEDDING_DIMENSION);
    assertThat(vector.model().modelName())
        .isEqualTo(AiProviderProperties.DEFAULT_EMBEDDING_MODEL_NAME);
    assertThat(vector.model().version())
        .isEqualTo(AiProviderProperties.DEFAULT_EMBEDDING_MODEL_VERSION);
  }

  @Test
  void rejectsEmbeddingWithoutAnAiExecutionContext() {
    assertThatThrownBy(() -> embeddings.embed("gel manicure"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
  }

  private static AiExecutionContext context() {
    UUID id = UUID.randomUUID();
    return new AiExecutionContext(
        UUID.randomUUID(), UUID.randomUUID(), Set.of("ROLE_CLIENT"), id, id, "trace", "idem");
  }
}
