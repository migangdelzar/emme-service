package com.emme.ai.platform.learning;

import com.emme.ai.contracts.learning.LearningCandidate;
import com.emme.kernel.context.AiExecutionContext;
import java.util.UUID;

/** Durable sink for candidates that passed the deterministic admission gate. */
@FunctionalInterface
public interface LearningCandidateStore {

  UUID save(LearningCandidate candidate, AiExecutionContext context);
}
