package com.emme.assistant.ai.configuration;

import com.emme.assistant.ai.adapter.out.workflow.ConversationWorkflowGraph;
import com.emme.assistant.ai.adapter.out.workflow.JdbcLangGraphCheckpointSaver;
import com.emme.assistant.ai.adapter.out.workflow.LangGraphConversationWorkflowAdapter;
import com.emme.assistant.ai.adapter.out.workflow.LangGraphQuoteWorkflowResumeAdapter;
import com.emme.assistant.ai.adapter.out.workflow.QuoteWorkflowGraph;
import com.emme.assistant.ai.adapter.out.workflow.TenantAwareCheckpointSaver;
import com.emme.assistant.ai.application.port.out.ConversationWorkflowPort;
import com.emme.assistant.ai.application.port.out.QuoteWorkflowResumePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.checkpoint.BaseCheckpointSaver;
import org.bsc.langgraph4j.state.AgentState;
import org.springframework.beans.factory.annotation.Qualifier;
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
  ConversationWorkflowGraph conversationWorkflowGraph(
      @Qualifier("aiLangGraphCheckpointSaver") BaseCheckpointSaver checkpointSaver) {
    return new ConversationWorkflowGraph(checkpointSaver);
  }

  @Bean(name = "aiConversationWorkflowCompiledGraph")
  @ConditionalOnMissingBean(name = "aiConversationWorkflowCompiledGraph")
  CompiledGraph<AgentState> conversationWorkflowCompiledGraph(ConversationWorkflowGraph graph)
      throws Exception {
    return graph.compile();
  }

  @Bean
  @ConditionalOnMissingBean
  ConversationWorkflowPort conversationWorkflowPort(
      @Qualifier("aiConversationWorkflowCompiledGraph") CompiledGraph<AgentState> graph) {
    return new LangGraphConversationWorkflowAdapter(graph);
  }

  @Bean
  @ConditionalOnMissingBean
  QuoteWorkflowGraph quoteWorkflowGraph(
      @Qualifier("aiLangGraphCheckpointSaver") BaseCheckpointSaver checkpointSaver) {
    return new QuoteWorkflowGraph(checkpointSaver);
  }

  @Bean(name = "aiQuoteWorkflowCompiledGraph")
  @ConditionalOnMissingBean(name = "aiQuoteWorkflowCompiledGraph")
  CompiledGraph<AgentState> quoteWorkflowCompiledGraph(QuoteWorkflowGraph graph) throws Exception {
    return graph.compile();
  }

  @Bean
  @ConditionalOnProperty(prefix = "app.ai.quote", name = "enabled", havingValue = "true")
  @ConditionalOnMissingBean
  QuoteWorkflowResumePort quoteWorkflowResumePort(
      @Qualifier("aiQuoteWorkflowCompiledGraph") CompiledGraph<AgentState> graph) {
    return new LangGraphQuoteWorkflowResumeAdapter(graph);
  }
}
