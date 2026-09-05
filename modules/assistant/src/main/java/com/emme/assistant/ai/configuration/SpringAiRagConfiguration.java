package com.emme.assistant.ai.configuration;

import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.ai.contracts.rag.KnowledgeRetriever;
import com.emme.assistant.ai.adapter.out.provider.springai.SpringAiQueryImprover;
import com.emme.assistant.ai.adapter.out.provider.springai.TenantScopedDocumentRetriever;
import com.emme.assistant.ai.adapter.out.provider.springai.advisor.PromptVersionAdvisor;
import com.emme.assistant.ai.adapter.out.provider.springai.advisor.TenantSecurityAdvisor;
import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.assistant.ai.application.port.out.IdentifiedChatCompletionPort;
import com.emme.assistant.ai.application.port.out.RagAnswerPort;
import com.emme.assistant.ai.application.provider.ChatModelSelector;
import com.emme.assistant.ai.application.provider.RagAnswerPolicy;
import com.emme.assistant.ai.application.rag.DeterministicRetrievalQualityGate;
import com.emme.assistant.ai.application.rag.KnowledgeAnswerService;
import com.emme.assistant.ai.application.rag.KnowledgeRoute;
import com.emme.assistant.ai.application.rag.QueryImprover;
import com.emme.assistant.ai.application.rag.RetrievalQualityGate;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

/**
 * Optional Spring AI RAG composition root.
 *
 * <p>RAG reuses the existing named chat providers, completion fallback selector, embedding port,
 * application-layer document search, and AI I/O executor. It is only active when chat and
 * embeddings are already configured, so the provider-neutral compatibility path remains available
 * when this feature is disabled.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SpringAiRagProperties.class)
@ConditionalOnProperty(prefix = "app.ai.spring-rag", name = "enabled", havingValue = "true")
@ConditionalOnBean({SpringAiChatProperties.class, EmbeddingModelPort.class})
public class SpringAiRagConfiguration {

  @Bean(name = "aiRagTaskExecutor")
  @ConditionalOnMissingBean(name = "aiRagTaskExecutor")
  TaskExecutor aiRagTaskExecutor(@Qualifier("aiIoExecutor") ExecutorService aiIoExecutor) {
    return command -> aiIoExecutor.execute(AiExecutionContextScope.captureCurrent(command));
  }

  @Bean
  @ConditionalOnMissingBean
  TenantScopedDocumentRetriever tenantScopedDocumentRetriever(
      KnowledgeRetriever retrieval, SpringAiRagProperties properties) {
    return new TenantScopedDocumentRetriever(retrieval, properties.retrievalLimit());
  }

  @Bean
  @ConditionalOnMissingBean
  RetrievalQualityGate retrievalQualityGate() {
    return new DeterministicRetrievalQualityGate();
  }

  @Bean
  @ConditionalOnMissingBean
  QueryImprover queryImprover(
      Map<String, ChatClient> chatClients,
      SpringAiChatProperties chatProperties,
      SpringAiRagProperties ragProperties) {
    ChatClient.Builder builder = configuredChatClient(chatClients, chatProperties).mutate();
    QueryTransformer compression =
        CompressionQueryTransformer.builder().chatClientBuilder(builder.clone()).build();
    QueryTransformer rewrite =
        RewriteQueryTransformer.builder()
            .chatClientBuilder(builder.clone())
            .targetSearchSystem("tenant knowledge documents")
            .build();
    QueryTransformer translation =
        TranslationQueryTransformer.builder()
            .chatClientBuilder(builder.clone())
            .targetLanguage("Spanish")
            .build();
    MultiQueryExpander expansion =
        MultiQueryExpander.builder()
            .chatClientBuilder(builder.clone())
            .includeOriginal(false)
            .numberOfQueries(ragProperties.improvement().maximumVariants())
            .build();
    return new SpringAiQueryImprover(compression, rewrite, translation, expansion);
  }

  @Bean
  @ConditionalOnMissingBean
  KnowledgeAnswerService knowledgeAnswerService(
      KnowledgeRetriever retrieval,
      RetrievalQualityGate qualityGate,
      QueryImprover queryImprover,
      @Qualifier("aiGroundedRagAnswer") RagAnswerPort answer,
      SpringAiRagProperties properties) {
    return new KnowledgeAnswerService(
        retrieval,
        qualityGate,
        queryImprover,
        answer,
        properties.quality().policy(KnowledgeRoute.GENERAL),
        properties.improvement().toPolicy());
  }

  @Bean(name = "aiRetrievalAugmentationAdvisor")
  @ConditionalOnMissingBean(name = "aiRetrievalAugmentationAdvisor")
  RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(
      TenantScopedDocumentRetriever documentRetriever,
      @Qualifier("aiRagTaskExecutor") TaskExecutor taskExecutor) {
    return RetrievalAugmentationAdvisor.builder()
        .documentRetriever(documentRetriever)
        .taskExecutor(taskExecutor)
        .build();
  }

  @Bean(name = "aiRagAnswer")
  @ConditionalOnMissingBean(name = "aiRagAnswer")
  RagAnswerPort ragAnswerPort(
      Map<String, ChatClient> chatClients,
      SpringAiChatProperties chatProperties,
      TenantSecurityAdvisor tenantSecurityAdvisor,
      PromptVersionAdvisor promptVersionAdvisor,
      RetrievalAugmentationAdvisor retrievalAugmentationAdvisor,
      AiTraceRecorder traceRecorder,
      Optional<ModelExecutionScheduler> scheduler,
      AiExecutorProperties executionProperties) {
    List<Advisor> advisors =
        SpringAiAdvisorConfiguration.orderedAdvisors(
            List.of(tenantSecurityAdvisor, promptVersionAdvisor, retrievalAugmentationAdvisor));
    var registry =
        new SpringAiChatProviderRegistry(chatClients, chatProperties, advisors, traceRecorder);
    ChatCompletionPort completions =
        scheduler
            .map(
                admission ->
                    (ChatCompletionPort)
                        new ChatModelSelector(
                            registry.providers(),
                            admission,
                            executionProperties.modelAdmissionTimeout()))
            .orElseGet(() -> new ChatModelSelector(registry.providers()));
    return new RagAnswerPolicy(completions);
  }

  @Bean(name = "aiGroundedRagAnswer")
  @ConditionalOnMissingBean(name = "aiGroundedRagAnswer")
  RagAnswerPort groundedRagAnswerPort(IdentifiedChatCompletionPort chatCompletion) {
    return new RagAnswerPolicy(chatCompletion);
  }

  private static ChatClient configuredChatClient(
      Map<String, ChatClient> chatClients, SpringAiChatProperties properties) {
    if (properties.providers().isEmpty()) {
      throw new IllegalArgumentException("At least one Spring AI chat provider is required");
    }
    String beanName = properties.providers().get(0).beanName();
    ChatClient client = chatClients.get(beanName);
    if (client == null) {
      throw new IllegalStateException("No Spring AI chat client bean configured for provider");
    }
    return client;
  }
}
