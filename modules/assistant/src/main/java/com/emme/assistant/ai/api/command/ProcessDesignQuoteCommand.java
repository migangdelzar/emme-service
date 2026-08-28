package com.emme.assistant.ai.api.command;

/** Client input for a design quote request; identity is resolved from backend context. */
public record ProcessDesignQuoteCommand(
    String templateKey, String inputText, String imageStorageKey) {

  public ProcessDesignQuoteCommand {
    requireText(templateKey, "templateKey");
    if ((inputText == null || inputText.isBlank())
        && (imageStorageKey == null || imageStorageKey.isBlank())) {
      throw new IllegalArgumentException("inputText or imageStorageKey must be provided");
    }
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
