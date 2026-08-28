package com.emme.assistant.ai.application.port.out;

import java.time.Duration;

/** Observability boundary for AI workflow lifecycle events. */
public interface AiWorkflowObserver {

  void workflowStarted(String workflowType);

  void workflowFinished(String workflowType, String outcome, Duration duration);
}
