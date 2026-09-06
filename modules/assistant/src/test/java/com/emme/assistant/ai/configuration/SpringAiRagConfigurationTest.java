package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emme.assistant.ai.adapter.out.provider.springai.TenantScopedDocumentRetriever;
import com.emme.assistant.ai.application.port.out.RagAnswerPort;
import com.emme.assistant.ai.application.provider.RagAnswerPolicy;
import com.emme.assistant.ai.application.rag.DeterministicRetrievalQualityGate;
import com.emme.assistant.ai.application.rag.KnowledgeAnswerService;
import com.emme.assistant.ai.application.rag.QueryImprover;
import com.emme.assistant.ai.application.rag.RetrievalQualityGate;
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
  void exposesTheProviderNeutralDeterministicRetrievalQualityGate() {
    SpringAiRagConfiguration configuration = new SpringAiRagConfiguration();

    assertThat(configuration.retrievalQualityGate())
        .isInstanceOf(DeterministicRetrievalQualityGate.class);
  }

  @Test
  void wiresBoundedSpringAiQueryImprovementFromTheConfiguredChatClient() {
    SpringAiRagConfiguration configuration = new SpringAiRagConfiguration();
    ChatClient client = mock(ChatClient.class);
    ChatClient.Builder builder = mock(ChatClient.Builder.class);
    org.mockito.Mockito.when(client.mutate()).thenReturn(builder);
    org.mockito.Mockito.when(builder.clone()).thenReturn(builder);
    SpringAiChatProperties properties =
        new SpringAiChatProperties(
            true, List.of(new SpringAiChatProperties.Provider("local", "local", "gemma4-v1")));

    QueryImprover queryImprover =
        configuration.queryImprover(
            Map.of("local", client), properties, new SpringAiRagProperties(true, 5));

    assertThat(queryImprover).isNotNull();
  }

  @Test
  void wiresKnowledgeAnswerServiceWithTheBoundedPolicies() {
    SpringAiRagConfiguration configuration = new SpringAiRagConfiguration();
    var retrieval = mock(com.emme.ai.contracts.rag.KnowledgeRetriever.class);
    RetrievalQualityGate gate = mock(RetrievalQualityGate.class);
    QueryImprover improver = mock(QueryImprover.class);
    RagAnswerPort answer = mock(RagAnswerPort.class);
    SpringAiRagProperties properties = new SpringAiRagProperties(true, 5);

    KnowledgeAnswerService service =
        configuration.knowledgeAnswerService(retrieval, gate, improver, answer, properties);

    assertThat(service).isNotNull();
  }

  @Test
  void reusesTheExistingNamedChatModelSelectorForRagFallback() {
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
            new SpringAiAdvisorConfiguration()
                .inputGuardAdvisor(new SpringAiAdvisorConfiguration().inputGuard()),
            new SpringAiAdvisorConfiguration()
                .outputGuardAdvisor(new SpringAiAdvisorConfiguration().outputGuard()),
            mock(RetrievalAugmentationAdvisor.class),
            mock(AiTraceRecorder.class),
            java.util.Optional.empty(),
            new AiExecutorProperties(2, 1, 1));

    assertThat(answerPort).isInstanceOf(RagAnswerPolicy.class);
  }

  private static AiExecutionContext context() {
    UUID id = UUID.randomUUID();
    return new AiExecutionContext(
        UUID.randomUUID(), UUID.randomUUID(), Set.of("ROLE_tenant_client"), id, id, "trace", "id");
  }
}
