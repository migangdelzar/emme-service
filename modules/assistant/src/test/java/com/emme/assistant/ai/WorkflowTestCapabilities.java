package com.emme.assistant.ai;

import com.emme.assistant.ai.application.port.out.ConversationWorkflowCapabilities;
import java.util.Map;

/** Minimal explicit capabilities for isolated workflow tests. */
public final class WorkflowTestCapabilities {

  private WorkflowTestCapabilities() {}

  public static ConversationWorkflowCapabilities basic() {
    ConversationWorkflowCapabilities.WorkflowStep empty =
        ConversationWorkflowCapabilities.WorkflowStep.empty();
    return new ConversationWorkflowCapabilities(
        request ->
            new ConversationWorkflowCapabilities.WorkflowStep(
                Map.of("intent", "GENERAL"), false, false, null),
        request -> empty,
        request ->
            new ConversationWorkflowCapabilities.WorkflowStep(
                Map.of("route", "GENERAL"), false, false, null),
        request -> empty,
        request -> empty,
        request -> empty,
        request -> empty,
        request ->
            new ConversationWorkflowCapabilities.WorkflowStep(
                Map.of("response", "Your request is ready."), false, false, null),
        request -> empty);
  }
}
