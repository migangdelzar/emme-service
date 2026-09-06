package com.emme.assistant.ai.application.workflow;

/** Guardrail boundaries enabled for one workflow node. */
public record NodeGuardrailPolicy(
    boolean checkInput,
    boolean checkContext,
    boolean checkTool,
    boolean checkGrounding,
    boolean checkOutput) {}
