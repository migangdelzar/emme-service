package com.emme.assistant.ai.application.port.out;

import com.emme.assistant.ai.domain.workflow.QuoteWorkflow;
import java.util.Optional;

/** Durable tenant-scoped quote workflow store with idempotent lookup. */
public interface QuoteWorkflowRepository {

  Optional<QuoteWorkflow> findByIdempotencyKey(String idempotencyKey);

  QuoteWorkflow save(QuoteWorkflow workflow);
}
