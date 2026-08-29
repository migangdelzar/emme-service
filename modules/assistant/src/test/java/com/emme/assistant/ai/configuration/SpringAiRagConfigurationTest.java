package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emme.assistant.ai.adapter.out.provider.springai.TenantScopedDocumentRetriever;
import com.emme.assistant.ai.application.port.out.RagAnswerPort;
import com.emme.assistant.ai.application.provider.RagAnswerProviderChain;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.core.task.TaskExecutor;

class SpringAiRagConfigurationTest {

  @Test
  void propagatesTheBackendAiContextThroughTheExistingAiIoExecutor() throws Exception {
    SpringAiRagConfiguration configuration = new SpringAiRagConfiguration();
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      TaskExecutor taskExecutor = configuration.aiRagTaskExecutor(executor);
      AiExecutionContext expected = context();
      AtomicReference<AiExecutionContext> observed = new AtomicReference<>();
      var completed = new java.util.concurrent.CountDownLatch(1);

      AiExecutionContextScope.run(
          expected,
          () ->
              taskExecutor.execute(
                  () -> {
                    observed.set(AiExecutionContextScope.requireCurrent());
                    completed.countDown();
                  }));

      assertThat(completed.await(2, TimeUnit.SECONDS)).isTrue();
      assertThat(observed.get()).isNotNull();
      assertThat(observed.get().tenantId()).isEqualTo(expected.tenantId());
    }
  }

  @Test
  void buildsSpringAiRetrievalAugmentationWithTheConfiguredTaskExecutor() {
    SpringAiRagConfiguration configuration = new SpringAiRagConfiguration();
    TenantScopedDocumentRetriever retriever = mock(TenantScopedDocumentRetriever.class);

    RetrievalAugmentationAdvisor advisor =
        configuration.retrievalAugmentationAdvisor(retriever, Runnable::run);

    assertThat(advisor).isNotNull();
  }

  @Test
  void reusesTheExistingNamedChatProviderChainForRagFallback() {
    SpringAiRagConfiguration configuration = new SpringAiRagConfiguration();
    SpringAiChatProperties chatProperties =
        new SpringAiChatProperties(
            true, List.of(new SpringAiChatProperties.Provider("local", "local", "gemma4-v1")));

    RagAnswerPort answerPort =
        configuration.ragAnswerPort(
            Map.of("local", mock(ChatClient.class)),
            chatProperties,
            new com.emme.assistant.ai.adapter.out.provider.springai.advisor.TenantSecurityAdvisor(),
            new com.emme.assistant.ai.adapter.out.provider.springai.advisor.PromptVersionAdvisor(
                "rag-v1"),
            mock(RetrievalAugmentationAdvisor.class),
            mock(AiTraceRecorder.class),
            java.util.Optional.empty(),
            new AiExecutorProperties(2, 1, 1));

    assertThat(answerPort).isInstanceOf(RagAnswerProviderChain.class);
  }

  private static AiExecutionContext context() {
    UUID id = UUID.randomUUID();
    return new AiExecutionContext(
        UUID.randomUUID(), UUID.randomUUID(), Set.of("ROLE_tenant_client"), id, id, "trace", "id");
  }
}
