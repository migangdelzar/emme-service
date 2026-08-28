package com.emme.assistant.ai.domain.workflow;

import com.emme.assistant.ai.domain.quote.QuoteCalculation;
import java.util.Objects;
import java.util.UUID;

/** Durable quote candidate produced by the deterministic calculator. */
public record QuoteDraft(
    UUID id, UUID tenantId, UUID workflowId, QuoteCalculation calculation, long version) {

  public QuoteDraft {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(tenantId, "tenantId must not be null");
    Objects.requireNonNull(workflowId, "workflowId must not be null");
    Objects.requireNonNull(calculation, "calculation must not be null");
    if (version < 0) {
      throw new IllegalArgumentException("version must not be negative");
    }
  }

  public static QuoteDraft create(
      UUID id, UUID tenantId, UUID workflowId, QuoteCalculation calculation) {
    return new QuoteDraft(id, tenantId, workflowId, calculation, 0);
  }
}
