package com.emme.assistant.ai.application.port.out;

import com.emme.assistant.ai.domain.workflow.QuoteDraft;
import com.emme.assistant.ai.domain.workflow.QuoteReviewTask;
import java.util.UUID;

/** Durable store for extraction, quote-draft, and HITL artifacts. */
public interface QuoteArtifactRepository {

  void saveExtraction(UUID workflowId, NailDesignExtractor.ExtractionResult extraction);

  void saveDraft(QuoteDraft draft);

  QuoteReviewTask saveReviewTask(QuoteReviewTask reviewTask);
}
