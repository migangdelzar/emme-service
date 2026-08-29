package com.emme.ai.contracts.learning;

/** Evidence captured from a completed interaction before candidate admission. */
public record LearningCandidateEvidence(
    boolean routeAccepted,
    boolean executionSucceeded,
    boolean outcomeValidated,
    boolean acceptedOutcome,
    boolean staffCorrected,
    boolean policyViolation,
    boolean piiRedacted) {}
