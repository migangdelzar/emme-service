package com.emme.assistant.ai.application.port.out;

import com.emme.assistant.ai.domain.workflow.QuoteReviewTask;
import java.util.Optional;
import java.util.UUID;

/** Tenant-scoped durable store for staff review tasks. */
public interface QuoteReviewRepository {

  Optional<QuoteReviewTask> findById(UUID reviewTaskId);

  QuoteReviewTask save(QuoteReviewTask reviewTask);
}
