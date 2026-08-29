package com.emme.assistant.ai.adapter.out.provider.springai;

import com.emme.assistant.ai.application.tool.AiToolDefinition;
import com.emme.assistant.ai.application.tool.AiToolGateway;
import com.emme.assistant.ai.application.tool.AiToolInvocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.Objects;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.core.ParameterizedTypeReference;

/** Exposes only gateway-approved read-only tools to Spring AI's native tool-calling advisor. */
public final class SpringAiToolCallbackProvider implements ToolCallbackProvider {

  private static final ParameterizedTypeReference<Map<String, String>> ARGUMENT_TYPE =
      new ParameterizedTypeReference<>() {};

  private final AiToolGateway gateway;
  private final ObjectMapper objectMapper;

  public SpringAiToolCallbackProvider(AiToolGateway gateway, ObjectMapper objectMapper) {
    this.gateway = Objects.requireNonNull(gateway, "gateway must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  public ToolCallback[] getToolCallbacks() {
    return gateway.agentEligibleToolDefinitions().stream()
        .sorted(java.util.Comparator.comparing(AiToolDefinition::key))
        .map(this::callback)
        .toArray(ToolCallback[]::new);
  }

  private ToolCallback callback(AiToolDefinition definition) {
    return FunctionToolCallback.<Map<String, String>, String>builder(
            definition.key(),
            (arguments, ignoredToolContext) ->
                gateway
                    .execute(new AiToolInvocation(definition.key(), arguments, false, false))
                    .content())
        .description(definition.description())
        .inputType(ARGUMENT_TYPE)
        .inputSchema(inputSchema(definition))
        .build();
  }

  private String inputSchema(AiToolDefinition definition) {
    ObjectNode schema = objectMapper.createObjectNode();
    schema.put("type", "object");
    ObjectNode properties = schema.putObject("properties");
    definition.allowedArgumentNames().stream()
        .sorted()
        .forEach(argument -> properties.putObject(argument).put("type", "string"));
    ArrayNode required = schema.putArray("required");
    definition.requiredArgumentNames().stream().sorted().forEach(required::add);
    schema.put("additionalProperties", false);
    try {
      return objectMapper.writeValueAsString(schema);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException(
          "Could not serialize AI tool schema: " + definition.key(), exception);
    }
  }
}
