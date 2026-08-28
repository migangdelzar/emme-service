package com.emme.assistant.ai.application.port.out;

import com.emme.assistant.ai.domain.workflow.QuoteWorkflow;
import java.util.Optional;
import java.util.UUID;

/** Durable tenant-scoped quote workflow store with idempotent lookup. */
public interface QuoteWorkflowRepository {

  Optional<QuoteWorkflow> findByIdempotencyKey(String idempotencyKey);

  Optional<QuoteWorkflow> findById(UUID workflowId);

  QuoteWorkflow save(QuoteWorkflow workflow);
}
