package com.emme.assistant.ai.application.trace;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** Metadata and outcome for one model execution attempt. Payload redaction is recorder-owned. */
public record AiModelExecutionTrace(
    UUID executionId,
    String operation,
    String providerKey,
    String modelVersion,
    String promptVersion,
    String graphVersion,
    AiExecutionStatus status,
    long latencyMillis,
    Integer inputTokens,
    Integer outputTokens,
    Integer totalTokens,
    BigDecimal estimatedCost,
    String requestPayload,
    String responsePayload,
    String errorCode,
    String errorMessage) {

  public AiModelExecutionTrace {
    Objects.requireNonNull(executionId, "executionId must not be null");
    requireText(operation, "operation");
    requireText(providerKey, "providerKey");
    requireText(modelVersion, "modelVersion");
    requireText(promptVersion, "promptVersion");
    Objects.requireNonNull(status, "status must not be null");
    if (latencyMillis < 0) {
      throw new IllegalArgumentException("latencyMillis must not be negative");
    }
    validateNonNegative(inputTokens, "inputTokens");
    validateNonNegative(outputTokens, "outputTokens");
    validateNonNegative(totalTokens, "totalTokens");
    if (estimatedCost != null && estimatedCost.signum() < 0) {
      throw new IllegalArgumentException("estimatedCost must not be negative");
    }
    requireText(requestPayload, "requestPayload");
    if (status == AiExecutionStatus.SUCCEEDED
        && (responsePayload == null || responsePayload.isBlank())) {
      throw new IllegalArgumentException("successful model execution requires a response payload");
    }
  }

  private static void validateNonNegative(Integer value, String field) {
    if (value != null && value < 0) {
      throw new IllegalArgumentException(field + " must not be negative");
    }
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
