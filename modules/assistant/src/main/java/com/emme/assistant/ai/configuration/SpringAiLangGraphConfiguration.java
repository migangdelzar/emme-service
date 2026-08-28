package com.emme.assistant.ai.configuration;

import com.emme.assistant.ai.adapter.out.workflow.JdbcLangGraphCheckpointSaver;
import com.emme.assistant.ai.adapter.out.workflow.QuoteWorkflowGraph;
import com.emme.assistant.ai.adapter.out.workflow.TenantAwareCheckpointSaver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bsc.langgraph4j.checkpoint.BaseCheckpointSaver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Opt-in composition root for the tenant-safe LangGraph4j workflow boundary. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LangGraphProperties.class)
@ConditionalOnProperty(prefix = "app.ai.langgraph", name = "enabled", havingValue = "true")
public class SpringAiLangGraphConfiguration {

  @Bean
  @ConditionalOnMissingBean
  JdbcLangGraphCheckpointSaver jdbcCheckpointSaver(JdbcClient jdbc, ObjectMapper objectMapper) {
    return new JdbcLangGraphCheckpointSaver(jdbc, objectMapper);
  }

  @Bean(name = "aiLangGraphCheckpointSaver")
  @ConditionalOnMissingBean(name = "aiLangGraphCheckpointSaver")
  BaseCheckpointSaver tenantAwareCheckpointSaver(JdbcLangGraphCheckpointSaver checkpointSaver) {
    return new TenantAwareCheckpointSaver(checkpointSaver);
  }

  @Bean
  @ConditionalOnMissingBean
  QuoteWorkflowGraph quoteWorkflowGraph(BaseCheckpointSaver checkpointSaver) {
    return new QuoteWorkflowGraph(checkpointSaver);
  }
}
