package com.emme.assistant.ai.configuration;

import com.emme.assistant.ai.application.tool.AiToolDefinition;
import com.emme.assistant.ai.application.tool.AiToolGateway;
import com.emme.assistant.ai.application.tool.AuthorizedAiToolGateway;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires typed tool definitions into the controlled in-process gateway. */
@Configuration(proxyBeanMethods = false)
public class SpringAiToolConfiguration {

  @Bean
  @ConditionalOnMissingBean(AiToolGateway.class)
  AiToolGateway aiToolGateway(List<AiToolDefinition> definitions) {
    return new AuthorizedAiToolGateway(definitions);
  }
}
