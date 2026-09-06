package com.emme.assistant.ai.configuration;

import com.emme.ai.contracts.embedding.EmbeddingService;
import com.emme.assistant.ai.application.port.out.SemanticMetrics;
import com.emme.assistant.ai.application.port.out.SemanticReferenceSearchPort;
import com.emme.assistant.ai.application.semantic.SemanticIntentClassifier;
import com.emme.assistant.ai.application.semantic.SemanticIntentRouter;
import com.emme.assistant.ai.application.semantic.SemanticMatchPolicy;
import com.emme.assistant.ai.application.semantic.SemanticToolSelector;
import com.emme.assistant.ai.application.tool.AiToolGateway;
import com.emme.assistant.ai.application.tool.SemanticProactiveToolRouter;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Opt-in composition root for deterministic pgvector-backed semantic routing. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SemanticRoutingProperties.class)
@ConditionalOnProperty(prefix = "app.ai.semantic-routing", name = "enabled", havingValue = "true")
@ConditionalOnBean({EmbeddingService.class, SemanticReferenceSearchPort.class})
public class SpringAiSemanticConfiguration {

  @Bean
  @ConditionalOnMissingBean
  SemanticMatchPolicy semanticMatchPolicy(SemanticRoutingProperties properties) {
    return new SemanticMatchPolicy(properties.minimumTop1Similarity(), properties.minimumMargin());
  }

  @Bean
  @ConditionalOnMissingBean
  SemanticIntentClassifier semanticIntentClassifier(
      SemanticReferenceSearchPort search,
      SemanticMatchPolicy policy,
      SemanticMetrics metrics,
      AiTraceRecorder traceRecorder) {
    return new SemanticIntentClassifier(search, policy, metrics, traceRecorder);
  }

  SemanticIntentClassifier semanticIntentClassifier(
      SemanticReferenceSearchPort search, SemanticMatchPolicy policy) {
    return new SemanticIntentClassifier(search, policy);
  }

  @Bean
  @ConditionalOnMissingBean
  SemanticToolSelector semanticToolSelector(
      SemanticReferenceSearchPort search,
      SemanticMatchPolicy policy,
      SemanticMetrics metrics,
      AiTraceRecorder traceRecorder) {
    return new SemanticToolSelector(search, policy, metrics, traceRecorder);
  }

  SemanticToolSelector semanticToolSelector(
      SemanticReferenceSearchPort search, SemanticMatchPolicy policy) {
    return new SemanticToolSelector(search, policy);
  }

  @Bean
  @ConditionalOnMissingBean
  SemanticProactiveToolRouter semanticProactiveToolRouter(
      SemanticToolSelector selector, AiToolGateway gateway, SemanticRoutingProperties properties) {
    return new SemanticProactiveToolRouter(selector, gateway, properties.locale());
  }

  @Bean
  @ConditionalOnMissingBean
  SemanticIntentRouter semanticIntentRouter(
      EmbeddingService embeddings,
      SemanticIntentClassifier classifier,
      SemanticRoutingProperties properties) {
    return new SemanticIntentRouter(embeddings, classifier, properties.locale());
  }
}
