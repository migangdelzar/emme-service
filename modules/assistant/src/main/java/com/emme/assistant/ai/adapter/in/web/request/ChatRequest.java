package com.emme.assistant.ai.adapter.in.web.request;

/** HTTP request for a conversational AI response. */
public record ChatRequest(String userMessage, String conversationContext) {}
