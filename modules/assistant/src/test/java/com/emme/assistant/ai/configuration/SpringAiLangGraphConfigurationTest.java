package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emme.assistant.ai.adapter.out.workflow.JdbcLangGraphCheckpointSaver;
import com.emme.assistant.ai.adapter.out.workflow.QuoteWorkflowGraph;
import com.emme.assistant.ai.adapter.out.workflow.TenantAwareCheckpointSaver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bsc.langgraph4j.checkpoint.BaseCheckpointSaver;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class SpringAiLangGraphConfigurationTest {

  @Test
  void wiresTheTenantAwarePostgresCheckpointBoundary() {
    SpringAiLangGraphConfiguration configuration = new SpringAiLangGraphConfiguration();
    JdbcLangGraphCheckpointSaver checkpointSaver =
        configuration.jdbcCheckpointSaver(mock(JdbcClient.class), new ObjectMapper());

    BaseCheckpointSaver tenantAwareSaver =
        configuration.tenantAwareCheckpointSaver(checkpointSaver);
    QuoteWorkflowGraph graph = configuration.quoteWorkflowGraph(tenantAwareSaver);

    assertThat(checkpointSaver).isNotNull();
    assertThat(tenantAwareSaver).isInstanceOf(TenantAwareCheckpointSaver.class);
    assertThat(graph).isNotNull();
  }

  @Test
  void providesStableGraphVersionDefaults() {
    assertThat(new LangGraphProperties(false, "").graphVersion()).isEqualTo("quote-v1");
  }
}
