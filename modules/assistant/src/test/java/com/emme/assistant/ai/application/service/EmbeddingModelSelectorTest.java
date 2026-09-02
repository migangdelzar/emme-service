package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.model.ModelCapability;
import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.assistant.ai.application.port.out.EmbeddingProviderUnavailableException;
import com.emme.assistant.ai.application.provider.EmbeddingModelSelector;
import com.emme.assistant.ai.application.semantic.EmbeddingVector;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;

class EmbeddingModelSelectorTest {

  private static final EmbeddingVector LOCAL_VECTOR =
      new EmbeddingVector("local-embeddinggemma:300m", List.of(0.1f, 0.9f));
  private static final EmbeddingVector CLOUD_VECTOR =
      new EmbeddingVector("cloud-text-embedding", List.of(0.2f, 0.8f));

  @Test
  void returnsThePrimaryProviderResultWithoutCallingFallbacks() {
    EmbeddingModelPort primary = mock(EmbeddingModelPort.class);
    EmbeddingModelPort fallback = mock(EmbeddingModelPort.class);
    when(primary.embed("book Friday afternoon")).thenReturn(LOCAL_VECTOR);
    EmbeddingModelSelector chain =
        new EmbeddingModelSelector(
            List.of(
                new EmbeddingModelSelector.Provider("local", primary),
                new EmbeddingModelSelector.Provider("cloud", fallback)));

    assertThat(chain.embed("book Friday afternoon")).isEqualTo(LOCAL_VECTOR);

    verifyNoInteractions(fallback);
  }

  @Test
  void usesTheNextProviderOnlyWhenTheCurrentProviderIsUnavailable() {
    EmbeddingModelPort primary = mock(EmbeddingModelPort.class);
    EmbeddingModelPort fallback = mock(EmbeddingModelPort.class);
    when(primary.embed("quote this design"))
        .thenThrow(new EmbeddingProviderUnavailableException("local unavailable"));
    when(fallback.embed("quote this design")).thenReturn(CLOUD_VECTOR);
    EmbeddingModelSelector chain =
        new EmbeddingModelSelector(
            List.of(
                new EmbeddingModelSelector.Provider("local", primary),
                new EmbeddingModelSelector.Provider("cloud", fallback)));

    assertThat(chain.embed("quote this design")).isEqualTo(CLOUD_VECTOR);
    var invocationOrder = inOrder(primary, fallback);
    invocationOrder.verify(primary).embed("quote this design");
    invocationOrder.verify(fallback).embed("quote this design");
  }

  @Test
  void doesNotFallbackWhenAProviderReturnsAnInvalidVector() {
    EmbeddingModelPort primary = mock(EmbeddingModelPort.class);
    EmbeddingModelPort fallback = mock(EmbeddingModelPort.class);
    when(primary.embed("quote this design"))
        .thenThrow(
            new IllegalStateException("Embedding dimension does not match configured dimension"));
    EmbeddingModelSelector chain =
        new EmbeddingModelSelector(
            List.of(
                new EmbeddingModelSelector.Provider("local", primary),
                new EmbeddingModelSelector.Provider("cloud", fallback)));

    assertThatThrownBy(() -> chain.embed("quote this design"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Embedding dimension does not match configured dimension");
    verifyNoInteractions(fallback);
  }

  @Test
  void rejectsBlankInputBeforeTouchingAnyProvider() {
    EmbeddingModelPort primary = mock(EmbeddingModelPort.class);
    EmbeddingModelSelector chain =
        new EmbeddingModelSelector(List.of(new EmbeddingModelSelector.Provider("local", primary)));

    assertThatThrownBy(() -> chain.embed(" "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Embedding text must not be blank");
    verifyNoInteractions(primary);
  }

  @Test
  void reportsWhenEveryProviderIsUnavailable() {
    EmbeddingModelPort primary = mock(EmbeddingModelPort.class);
    EmbeddingModelPort fallback = mock(EmbeddingModelPort.class);
    when(primary.embed("faq"))
        .thenThrow(new EmbeddingProviderUnavailableException("local unavailable"));
    when(fallback.embed("faq"))
        .thenThrow(new EmbeddingProviderUnavailableException("cloud unavailable"));
    EmbeddingModelSelector chain =
        new EmbeddingModelSelector(
            List.of(
                new EmbeddingModelSelector.Provider("local", primary),
                new EmbeddingModelSelector.Provider("cloud", fallback)));

    assertThatThrownBy(() -> chain.embed("faq"))
        .isInstanceOf(EmbeddingProviderUnavailableException.class)
        .hasMessage("All configured embedding providers are unavailable: local, cloud");
    var invocationOrder = inOrder(primary, fallback);
    invocationOrder.verify(primary).embed("faq");
    invocationOrder.verify(fallback).embed("faq");
  }

  @Test
  void admitsEachProviderAttemptThroughTheExistingModelScheduler() {
    EmbeddingModelPort primary = mock(EmbeddingModelPort.class);
    when(primary.embed("faq")).thenReturn(LOCAL_VECTOR);
    var scheduler = new RecordingScheduler();
    EmbeddingModelSelector chain =
        new EmbeddingModelSelector(
            List.of(new EmbeddingModelSelector.Provider("local", primary)),
            scheduler,
            Duration.ofSeconds(1));

    EmbeddingVector result = AiExecutionContextScope.call(context(), () -> chain.embed("faq"));

    assertThat(result).isEqualTo(LOCAL_VECTOR);
    assertThat(scheduler.capabilities).containsExactly(ModelCapability.EMBEDDING);
  }

  @Test
  void preservesFallbackOrderAndAdmissionForEachEmbeddingAttempt() {
    EmbeddingModelPort primary = mock(EmbeddingModelPort.class);
    EmbeddingModelPort fallback = mock(EmbeddingModelPort.class);
    when(primary.embed("faq"))
        .thenThrow(new EmbeddingProviderUnavailableException("local unavailable"));
    when(fallback.embed("faq")).thenReturn(CLOUD_VECTOR);
    var scheduler = new RecordingScheduler();
    EmbeddingModelSelector selector =
        new EmbeddingModelSelector(
            List.of(
                new EmbeddingModelSelector.Provider("local", primary),
                new EmbeddingModelSelector.Provider("cloud", fallback)),
            scheduler,
            Duration.ofSeconds(1));

    EmbeddingVector result = AiExecutionContextScope.call(context(), () -> selector.embed("faq"));

    assertThat(result).isEqualTo(CLOUD_VECTOR);
    assertThat(scheduler.capabilities)
        .containsExactly(ModelCapability.EMBEDDING, ModelCapability.EMBEDDING);
    var invocationOrder = inOrder(primary, fallback);
    invocationOrder.verify(primary).embed("faq");
    invocationOrder.verify(fallback).embed("faq");
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Set.of("CLIENT"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-embedding",
        "idempotency-embedding");
  }

  private static final class RecordingScheduler implements ModelExecutionScheduler {
    private final List<ModelCapability> capabilities = new java.util.ArrayList<>();

    @Override
    public <T> T execute(
        ModelCapability capability,
        AiExecutionContext context,
        Duration timeout,
        Callable<T> operation) {
      capabilities.add(capability);
      try {
        return operation.call();
      } catch (RuntimeException exception) {
        throw exception;
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    }
  }
}
