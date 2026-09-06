package com.emme.ai.contracts.guardrail;

/** Typed outcomes produced by an AI safety boundary. */
public enum GuardrailAction {
  ALLOW,
  REDACT,
  CLARIFY,
  DENY,
  BLOCK,
  ESCALATE,
  REGENERATE,
  NO_ANSWER,
  DELIVER
}
