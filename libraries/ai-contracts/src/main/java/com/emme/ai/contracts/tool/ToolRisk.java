package com.emme.ai.contracts.tool;

/**
 * Compatibility risk value retained only for intent-definition callers.
 *
 * @deprecated assistant-owned tool execution uses {@code AiToolRisk}
 */
@Deprecated
public enum ToolRisk {
  READ_ONLY,
  MUTATION
}
