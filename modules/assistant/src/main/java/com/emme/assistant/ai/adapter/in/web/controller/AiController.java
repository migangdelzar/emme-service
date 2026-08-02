package com.emme.assistant.ai.adapter.in.web.controller;

import static com.emme.kernel.context.TenantContextHolder.withCurrentTenant;

import com.emme.assistant.ai.adapter.in.web.request.ChatRequest;
import com.emme.assistant.ai.adapter.in.web.request.IntentRequest;
import com.emme.assistant.ai.adapter.in.web.request.RagRequest;
import com.emme.assistant.ai.adapter.in.web.response.ChatResponse;
import com.emme.assistant.ai.adapter.in.web.response.RagResponse;
import com.emme.assistant.ai.api.result.IntentInfo;
import com.emme.assistant.ai.api.usecase.ChatUseCase;
import com.emme.assistant.ai.api.usecase.DetectIntentUseCase;
import com.emme.assistant.ai.api.usecase.RagQueryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI")
public class AiController {

  private final ChatUseCase chatUseCase;
  private final DetectIntentUseCase detectIntent;
  private final RagQueryUseCase ragQuery;

  public AiController(
      ChatUseCase chatUseCase, DetectIntentUseCase detectIntent, RagQueryUseCase ragQuery) {
    this.chatUseCase = chatUseCase;
    this.detectIntent = detectIntent;
    this.ragQuery = ragQuery;
  }

  @PostMapping("/chat")
  @Operation(summary = "Chat endpoint for testing AI responses")
  @PreAuthorize("@featureFlagService.isEnabled('ai_chat')")
  public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
    String response =
        chatUseCase.chat(
            request.conversationContext() != null ? request.conversationContext() : "",
            request.userMessage());
    return ResponseEntity.ok(new ChatResponse(response));
  }

  @PostMapping("/intent")
  @Operation(summary = "Detect intent from user message")
  @PreAuthorize("@featureFlagService.isEnabled('ai_chat')")
  public ResponseEntity<IntentInfo> detectIntent(@RequestBody IntentRequest request) {
    IntentInfo result = detectIntent.detect(request.message());
    return ResponseEntity.ok(result);
  }

  @PostMapping("/rag")
  @Operation(summary = "RAG query against tenant documents")
  @PreAuthorize("@featureFlagService.isEnabled('ai_chat')")
  public ResponseEntity<RagResponse> ragQuery(@RequestBody RagRequest request) {
    return withCurrentTenant(
        tenantId -> {
          String answer = ragQuery.query(tenantId, request.question());
          return ResponseEntity.ok(new RagResponse(answer));
        });
  }
}
