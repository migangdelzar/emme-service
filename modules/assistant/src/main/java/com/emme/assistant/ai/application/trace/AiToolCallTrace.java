package com.emme.assistant.ai.application.trace;

import java.util.Objects;
import java.util.UUID;

/**
 * Metadata and outcome for one backend-controlled tool attempt. Payload redaction is
 * recorder-owned.
 */
public record AiToolCallTrace(
    UUID callId,
    String toolKey,
    String riskLevel,
    AiToolCallStatus status,
    boolean authorized,
    boolean userConfirmed,
    boolean staffApproved,
    long latencyMillis,
    String argumentsPayload,
    String resultPayload,
    String errorCode,
    String errorMessage) {

  public AiToolCallTrace {
    Objects.requireNonNull(callId, "callId must not be null");
    requireText(toolKey, "toolKey");
    requireText(riskLevel, "riskLevel");
    Objects.requireNonNull(status, "status must not be null");
    if (latencyMillis < 0) {
      throw new IllegalArgumentException("latencyMillis must not be negative");
    }
    requireText(argumentsPayload, "argumentsPayload");
    if (status == AiToolCallStatus.SUCCEEDED
        && (resultPayload == null || resultPayload.isBlank())) {
      throw new IllegalArgumentException("successful tool call requires a result payload");
    }
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
