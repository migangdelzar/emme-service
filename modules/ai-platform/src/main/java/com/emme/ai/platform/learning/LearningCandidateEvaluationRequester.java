package com.emme.ai.platform.learning;

import com.emme.ai.contracts.learning.LearningCandidateEvaluationRequest;

/** Port for dispatching a candidate to the asynchronous evaluation boundary. */
@FunctionalInterface
public interface LearningCandidateEvaluationRequester {

  void request(LearningCandidateEvaluationRequest request);
}
