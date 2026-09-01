package com.emme.assistant.ai.configuration;

import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.ai.contracts.rag.KnowledgeSearch;
import com.emme.assistant.ai.adapter.out.provider.springai.TenantScopedDocumentRetriever;
import com.emme.assistant.ai.adapter.out.provider.springai.advisor.PromptVersionAdvisor;
import com.emme.assistant.ai.adapter.out.provider.springai.advisor.TenantSecurityAdvisor;
import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.assistant.ai.application.port.out.RagAnswerPort;
import com.emme.assistant.ai.application.provider.ChatProviderChain;
import com.emme.assistant.ai.application.provider.RagAnswerProviderChain;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
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
 * <p>RAG reuses the existing named chat providers, completion fallback chain, embedding port,
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
      KnowledgeSearch retrieval, SpringAiRagProperties properties) {
    return new TenantScopedDocumentRetriever(retrieval, properties.retrievalLimit());
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
      KnowledgeSearch retrieval,
      AiTraceRecorder traceRecorder,
      Optional<ModelExecutionScheduler> scheduler,
      AiExecutorProperties executionProperties) {
    List<Advisor> advisors =
        List.of(tenantSecurityAdvisor, promptVersionAdvisor, retrievalAugmentationAdvisor);
    var registry =
        new SpringAiChatProviderRegistry(chatClients, chatProperties, advisors, traceRecorder);
    ChatCompletionPort completions =
        scheduler
            .map(
                admission ->
                    (ChatCompletionPort)
                        new ChatProviderChain(
                            registry.providers(),
                            admission,
                            executionProperties.modelAdmissionTimeout()))
            .orElseGet(() -> new ChatProviderChain(registry.providers()));
    return new RagAnswerProviderChain(completions, retrieval);
  }
}
