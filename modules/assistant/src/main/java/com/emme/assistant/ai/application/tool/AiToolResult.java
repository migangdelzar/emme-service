package com.emme.assistant.ai.application.tool;

/** Result returned by a controlled application tool. */
public record AiToolResult(String toolKey, String content, boolean authoritative) {

  public AiToolResult {
    if (toolKey == null || toolKey.isBlank()) {
      throw new IllegalArgumentException("toolKey must not be blank");
    }
    if (content == null || content.isBlank()) {
      throw new IllegalArgumentException("content must not be blank");
    }
  }
}
