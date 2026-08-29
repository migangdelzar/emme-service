package com.emme.ai.platform.learning;

import com.emme.ai.contracts.learning.LearningCandidateEvaluationReport;
import com.emme.kernel.context.AiExecutionContext;
import java.util.UUID;

/** Durable sink for offline evaluation evidence before lifecycle transitions. */
@FunctionalInterface
public interface LearningCandidateEvaluationStore {

  UUID save(UUID candidateId, LearningCandidateEvaluationReport report, AiExecutionContext context);
}
