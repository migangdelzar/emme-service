package com.emme.assistant.ai.configuration;

import com.emme.ai.contracts.semantic.SemanticCacheDependencyPublisher;
import com.emme.assistant.ai.adapter.out.event.NoopSemanticCacheDependencyPublisher;
import com.emme.assistant.ai.adapter.out.event.SpringSemanticCacheDependencyPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Supplies the dependency publisher independently of the optional semantic-cache runtime. */
@Configuration(proxyBeanMethods = false)
public class SemanticCacheDependencyPublisherConfiguration {

  @Bean
  @ConditionalOnProperty(prefix = "app.ai.semantic-cache", name = "enabled", havingValue = "true")
  @ConditionalOnMissingBean(SemanticCacheDependencyPublisher.class)
  SemanticCacheDependencyPublisher semanticCacheDependencyPublisher(
      ApplicationEventPublisher events) {
    return new SpringSemanticCacheDependencyPublisher(events);
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "app.ai.semantic-cache",
      name = "enabled",
      havingValue = "false",
      matchIfMissing = true)
  @ConditionalOnMissingBean(SemanticCacheDependencyPublisher.class)
  SemanticCacheDependencyPublisher noopSemanticCacheDependencyPublisher() {
    return new NoopSemanticCacheDependencyPublisher();
  }
}
