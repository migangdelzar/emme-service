package com.emme.assistant.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.assistant.ai.configuration.SpringAiLangGraphConfiguration;
import com.emme.testing.integration.annotation.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = LangGraphDisabledStartupIntegrationTest.DisabledStartupApplication.class)
@TestPropertySource(properties = {"app.ai.langgraph.enabled=false", "app.ai.quote.enabled=true"})
@PostgresIntegrationTest
class LangGraphDisabledStartupIntegrationTest {

  @Autowired private ApplicationContext context;

  @Test
  void startsWithoutLangGraphCheckpointOrCompiledGraphBeans() {
    assertThat(context.containsBean("workflowCheckpointStore")).isFalse();
    assertThat(context.containsBean("aiConversationWorkflowCompiledGraph")).isFalse();
    assertThat(context.containsBean("aiQuoteWorkflowCompiledGraph")).isFalse();
  }

  @SpringBootConfiguration(proxyBeanMethods = false)
  @EnableAutoConfiguration
  @Import(SpringAiLangGraphConfiguration.class)
  static class DisabledStartupApplication {}
}
