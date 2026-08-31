package com.emme.assistant.ai.domain.job;

public enum AiJobStatus {
  QUEUED,
  CLAIMED,
  COMPLETED,
  RETRYING,
  DEAD_LETTER,
  NOT_FOUND,
  NOT_AVAILABLE,
  TENANT_MISMATCH
}
