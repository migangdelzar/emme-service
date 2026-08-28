package com.emme.assistant.ai.adapter.out.persistence;

import com.emme.assistant.ai.application.port.out.SemanticCachePayloadCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import java.util.Optional;

/** Jackson adapter for the JSONB semantic-cache response payload. */
public final class JacksonSemanticCachePayloadCodec implements SemanticCachePayloadCodec {

  private final ObjectMapper objectMapper;

  public JacksonSemanticCachePayloadCodec(ObjectMapper objectMapper) {
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  public String encodeText(String response) {
    if (response == null || response.isBlank()) {
      throw new IllegalArgumentException("response must not be blank");
    }
    try {
      return objectMapper.createObjectNode().put("text", response).toString();
    } catch (RuntimeException exception) {
      throw new IllegalStateException("Could not encode semantic-cache response", exception);
    }
  }

  @Override
  public Optional<String> decodeText(String payload) {
    if (payload == null || payload.isBlank()) {
      return Optional.empty();
    }
    try {
      JsonNode node = objectMapper.readTree(payload);
      JsonNode text = node == null ? null : node.get("text");
      return text == null || !text.isTextual() || text.textValue().isBlank()
          ? Optional.empty()
          : Optional.of(text.textValue());
    } catch (Exception exception) {
      return Optional.empty();
    }
  }
}
