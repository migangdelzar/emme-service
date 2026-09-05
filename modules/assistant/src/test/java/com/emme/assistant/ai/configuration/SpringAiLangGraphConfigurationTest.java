package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.emme.assistant.ai.WorkflowTestCapabilities;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.simple.JdbcClient;

class SpringAiLangGraphConfigurationTest {

  @Test
  void wiresTheTenantAwarePostgresCheckpointBoundary() throws Exception {
    SpringAiLangGraphConfiguration configuration = new SpringAiLangGraphConfiguration();
    JdbcLangGraphCheckpointSaver checkpointSaver =
        configuration.jdbcCheckpointSaver(mock(JdbcClient.class), new ObjectMapper());

    BaseCheckpointSaver tenantAwareSaver = configuration.workflowCheckpointStore(checkpointSaver);
    ConversationWorkflowGraph conversationGraph =
        configuration.conversationWorkflowGraph(tenantAwareSaver, WorkflowTestCapabilities.basic());
    CompiledGraph<AgentState> compiledConversationGraph =
        configuration.conversationWorkflowCompiledGraph(conversationGraph);
    ConversationWorkflowPort conversationWorkflowPort =
        configuration.conversationWorkflowPort(compiledConversationGraph);
    QuoteWorkflowGraph graph = configuration.quoteWorkflowGraph(tenantAwareSaver);
    CompiledGraph<AgentState> compiledGraph = configuration.quoteWorkflowCompiledGraph(graph);
    QuoteWorkflowResumePort resumePort = configuration.quoteWorkflowResumePort(compiledGraph);

    assertThat(checkpointSaver).isNotNull();
    assertThat(tenantAwareSaver).isInstanceOf(TenantAwareCheckpointSaver.class);
    assertThat(conversationGraph).isNotNull();
    assertThat(compiledConversationGraph).isNotNull();
    assertThat(conversationWorkflowPort).isInstanceOf(LangGraphConversationWorkflowAdapter.class);
    assertThat(graph).isNotNull();
    assertThat(compiledGraph).isNotNull();
    assertThat(resumePort).isInstanceOf(LangGraphQuoteWorkflowResumeAdapter.class);
  }

  @Test
  void providesStableGraphVersionDefaults() {
    assertThat(new LangGraphProperties(false, "").graphVersion()).isEqualTo("quote-v1");
  }

  @Test
  void failsFastWhenEnabledWorkflowCapabilitiesAreNotProvided() {
    SpringAiLangGraphConfiguration configuration = new SpringAiLangGraphConfiguration();

    assertThatThrownBy(configuration::conversationWorkflowCapabilities)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Conversation workflow capabilities must be provided");
  }

  @Test
  void selectsTheTenantAwareCheckpointSaverWhenMultipleSaversAreRegistered() throws Exception {
    var method =
        SpringAiLangGraphConfiguration.class.getDeclaredMethod(
            "quoteWorkflowGraph", BaseCheckpointSaver.class);

    assertThat(method.getParameters()[0].getAnnotation(Qualifier.class).value())
        .isEqualTo("workflowCheckpointStore");
  }

  @Test
  void selectsTheTenantScopedJdbcClientForCheckpoints() throws Exception {
    var method =
        SpringAiLangGraphConfiguration.class.getDeclaredMethod(
            "jdbcCheckpointSaver", JdbcClient.class, ObjectMapper.class);

    assertThat(method.getParameters()[0].getAnnotation(Qualifier.class).value())
        .isEqualTo("tenantJdbcClient");
  }

  @Test
  void selectsTheNamedCompiledGraphForEachWorkflowAdapter() throws Exception {
    var conversationMethod =
        SpringAiLangGraphConfiguration.class.getDeclaredMethod(
            "conversationWorkflowPort", CompiledGraph.class);
    var quoteMethod =
        SpringAiLangGraphConfiguration.class.getDeclaredMethod(
            "quoteWorkflowResumePort", CompiledGraph.class);

    assertThat(conversationMethod.getParameters()[0].getAnnotation(Qualifier.class).value())
        .isEqualTo("aiConversationWorkflowCompiledGraph");
    assertThat(quoteMethod.getParameters()[0].getAnnotation(Qualifier.class).value())
        .isEqualTo("aiQuoteWorkflowCompiledGraph");
  }

  @Test
  void createsQuoteGraphBeansOnlyWhenTheQuoteCapabilityIsEnabled() throws Exception {
    var quoteGraphMethod =
        SpringAiLangGraphConfiguration.class.getDeclaredMethod(
            "quoteWorkflowGraph", BaseCheckpointSaver.class);
    var compiledGraphMethod =
        SpringAiLangGraphConfiguration.class.getDeclaredMethod(
            "quoteWorkflowCompiledGraph", QuoteWorkflowGraph.class);

    assertThat(quoteGraphMethod.getAnnotation(ConditionalOnProperty.class))
        .satisfies(
            condition -> {
              assertThat(condition.prefix()).isEqualTo("app.ai.quote");
              assertThat(condition.name()).containsExactly("enabled");
              assertThat(condition.havingValue()).isEqualTo("true");
            });
    assertThat(compiledGraphMethod.getAnnotation(ConditionalOnProperty.class))
        .satisfies(
            condition -> {
              assertThat(condition.prefix()).isEqualTo("app.ai.quote");
              assertThat(condition.name()).containsExactly("enabled");
              assertThat(condition.havingValue()).isEqualTo("true");
            });
  }

  @Test
  void doesNotStartLangGraphBeansWhenTheFeatureIsDisabled() {
    new ApplicationContextRunner()
        .withUserConfiguration(SpringAiLangGraphConfiguration.class)
        .withPropertyValues("app.ai.langgraph.enabled=false", "app.ai.quote.enabled=true")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).doesNotHaveBean("workflowCheckpointStore");
              assertThat(context).doesNotHaveBean("aiConversationWorkflowCompiledGraph");
              assertThat(context).doesNotHaveBean("aiQuoteWorkflowCompiledGraph");
            });
  }

  @Test
  void exposesOneNamedCompiledGraphPerEnabledCapability() {
    new ApplicationContextRunner()
        .withUserConfiguration(SpringAiLangGraphConfiguration.class)
        .withPropertyValues("app.ai.langgraph.enabled=true", "app.ai.quote.enabled=true")
        .withBean("tenantJdbcClient", JdbcClient.class, () -> mock(JdbcClient.class))
        .withBean(ObjectMapper.class, ObjectMapper::new)
        .withBean(
            com.emme.assistant.ai.application.port.out.ConversationWorkflowCapabilities.class,
            () -> WorkflowTestCapabilities.basic())
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).hasBean("aiConversationWorkflowCompiledGraph");
              assertThat(context).hasBean("aiQuoteWorkflowCompiledGraph");
              assertThat(context.getBeansOfType(CompiledGraph.class)).hasSize(2);
            });
  }
}
