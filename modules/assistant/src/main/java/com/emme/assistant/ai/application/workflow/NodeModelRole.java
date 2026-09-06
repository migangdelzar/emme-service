package com.emme.assistant.ai.application.workflow;

/** The model capability, if any, used by a workflow node. */
public enum NodeModelRole {
  NONE,
  ROUTER,
  EXTRACTOR,
  ANSWER,
  REVIEW
}
