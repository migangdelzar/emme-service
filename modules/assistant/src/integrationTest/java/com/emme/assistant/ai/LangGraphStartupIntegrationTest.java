package com.emme.assistant.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emme.ai.contracts.image.TenantImageWriter;
import com.emme.assistant.ai.application.port.out.ConversationWorkflowCapabilities;
import com.emme.assistant.ai.configuration.SpringAiLangGraphConfiguration;
import com.emme.testing.integration.annotation.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import javax.sql.DataSource;
import org.bsc.langgraph4j.CompiledGraph;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = LangGraphStartupIntegrationTest.LangGraphStartupApplication.class)
@TestPropertySource(
    properties = {
      "app.ai.langgraph.enabled=true",
      "app.ai.quote.enabled=true",
      "spring.main.allow-bean-definition-overriding=true"
    })
@PostgresIntegrationTest
class LangGraphStartupIntegrationTest {

  @Autowired private ApplicationContext context;

  @Test
  void startsOneNamedCompiledGraphPerEnabledWorkflowCapability() {
    assertThat(context.containsBean("workflowCheckpointStore")).isTrue();
    assertThat(context.containsBean("aiConversationWorkflowCompiledGraph")).isTrue();
    assertThat(context.containsBean("aiQuoteWorkflowCompiledGraph")).isTrue();
    assertThat(context.getBeansOfType(CompiledGraph.class)).hasSize(2);
  }

  @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
  static class TestCapabilitiesConfiguration {

    @org.springframework.context.annotation.Bean
    ConversationWorkflowCapabilities conversationWorkflowCapabilities() {
      ConversationWorkflowCapabilities.WorkflowStep empty =
          ConversationWorkflowCapabilities.WorkflowStep.empty();
      return new ConversationWorkflowCapabilities(
          request ->
              new ConversationWorkflowCapabilities.WorkflowStep(
                  Map.of("intent", "GENERAL"), false, false, null),
          request -> empty,
          request ->
              new ConversationWorkflowCapabilities.WorkflowStep(
                  Map.of("route", "GENERAL"), false, false, null),
          request -> empty,
          request -> empty,
          request -> empty,
          request -> empty,
          request ->
              new ConversationWorkflowCapabilities.WorkflowStep(
                  Map.of("response", "Your request is ready."), false, false, null),
          request -> empty);
    }

    @Bean
    TenantImageWriter tenantImageWriter() {
      return mock(TenantImageWriter.class);
    }

    @Bean(name = "tenantJdbcClient")
    JdbcClient tenantJdbcClient(DataSource dataSource) {
      return JdbcClient.create(dataSource);
    }

    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper();
    }
  }

  @SpringBootConfiguration(proxyBeanMethods = false)
  @EnableAutoConfiguration
  @Import({SpringAiLangGraphConfiguration.class, TestCapabilitiesConfiguration.class})
  static class LangGraphStartupApplication {}
}
