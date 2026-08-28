package com.emme.assistant.ai.configuration;

import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.assistant.ai.application.port.out.SemanticReferenceSearchPort;
import com.emme.assistant.ai.application.service.SemanticIntentClassifier;
import com.emme.assistant.ai.application.service.SemanticIntentRouter;
import com.emme.assistant.ai.application.service.SemanticMatchPolicy;
import com.emme.assistant.ai.application.service.SemanticToolSelector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Opt-in composition root for deterministic pgvector-backed semantic routing. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SemanticRoutingProperties.class)
@ConditionalOnProperty(prefix = "app.ai.semantic-routing", name = "enabled", havingValue = "true")
public class SpringAiSemanticConfiguration {

  @Bean
  @ConditionalOnMissingBean
  SemanticMatchPolicy semanticMatchPolicy(SemanticRoutingProperties properties) {
    return new SemanticMatchPolicy(properties.minimumTop1Similarity(), properties.minimumMargin());
  }

  @Bean
  @ConditionalOnMissingBean
  SemanticIntentClassifier semanticIntentClassifier(
      SemanticReferenceSearchPort search, SemanticMatchPolicy policy) {
    return new SemanticIntentClassifier(search, policy);
  }

  @Bean
  @ConditionalOnMissingBean
  SemanticToolSelector semanticToolSelector(
      SemanticReferenceSearchPort search, SemanticMatchPolicy policy) {
    return new SemanticToolSelector(search, policy);
  }

  @Bean
  @ConditionalOnMissingBean
  SemanticIntentRouter semanticIntentRouter(
      EmbeddingModelPort embeddings,
      SemanticIntentClassifier classifier,
      SemanticRoutingProperties properties) {
    return new SemanticIntentRouter(embeddings, classifier, properties.locale());
  }
}
