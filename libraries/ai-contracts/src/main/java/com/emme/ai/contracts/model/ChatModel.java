package com.emme.ai.contracts.model;

/** Canonical provider-neutral chat capability. */
public interface ChatModel {

  /** Completes a prepared conversation request. */
  String complete(String conversationContext, String userMessage);
}
