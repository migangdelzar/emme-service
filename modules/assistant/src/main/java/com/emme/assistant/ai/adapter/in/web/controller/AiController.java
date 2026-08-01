package com.emme.assistant.ai.adapter.in.web.controller;

import static com.emme.kernel.context.TenantContextHolder.withCurrentTenant;

import com.emme.assistant.ai.api.usecase.ChatUseCase;
import com.emme.assistant.ai.application.AiService;
import com.emme.assistant.ai.application.ModelProvider.IntentResult;
import com.emme.assistant.ai.application.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
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
  private final AiService aiService;
  private final RagService ragService;

  public AiController(ChatUseCase chatUseCase, AiService aiService, RagService ragService) {
    this.chatUseCase = chatUseCase;
    this.aiService = aiService;
    this.ragService = ragService;
  }

  @PostMapping("/chat")
  @Operation(summary = "Chat endpoint for testing AI responses")
  @PreAuthorize("@featureFlagService.isEnabled('ai_chat')")
  public ResponseEntity<Map<String, String>> chat(@RequestBody ChatRequest request) {
    String response =
        chatUseCase.chat(
            request.conversationContext() != null ? request.conversationContext() : "",
            request.userMessage());
    return ResponseEntity.ok(Map.of("response", response));
  }

  @PostMapping("/intent")
  @Operation(summary = "Detect intent from user message")
  @PreAuthorize("@featureFlagService.isEnabled('ai_chat')")
  public ResponseEntity<IntentResult> detectIntent(@RequestBody IntentRequest request) {
    IntentResult result = aiService.routeIntent(request.message());
    return ResponseEntity.ok(result);
  }

  @PostMapping("/rag")
  @Operation(summary = "RAG query against tenant documents")
  @PreAuthorize("@featureFlagService.isEnabled('ai_chat')")
  public ResponseEntity<Map<String, String>> ragQuery(@RequestBody RagRequest request) {
    return withCurrentTenant(
        tenantId -> {
          String answer = ragService.query(tenantId, request.question());
          return ResponseEntity.ok(Map.of("answer", answer));
        });
  }

  // --- DTOs ---

  public record ChatRequest(String userMessage, String conversationContext) {}

  public record IntentRequest(String message) {}

  public record RagRequest(String question) {}
}
