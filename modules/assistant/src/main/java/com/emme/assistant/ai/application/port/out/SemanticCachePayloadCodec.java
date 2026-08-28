package com.emme.assistant.ai.application.port.out;

import java.util.Optional;

/** Encodes durable cache payloads without leaking serialization into application policy. */
public interface SemanticCachePayloadCodec {

  String encodeText(String response);

  Optional<String> decodeText(String payload);
}
