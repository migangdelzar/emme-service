package com.emme.assistant.web;

import static com.emme.kernel.context.TenantContextHolder.withCurrentTenant;

import com.emme.assistant.application.ConversationService;
import com.emme.assistant.entity.ActionType;
import com.emme.assistant.entity.Conversation;
import com.emme.assistant.entity.ConversationEvent;
import com.emme.assistant.entity.PendingAction;
import com.emme.kernel.type.ChannelType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/conversations")
@Tag(name = "Conversations")
public class ConversationController {

  private final ConversationService conversationService;

  public ConversationController(ConversationService conversationService) {
    this.conversationService = conversationService;
  }

  @PostMapping
  @Operation(summary = "Start a new conversation")
  public ResponseEntity<ConversationResponse> start(@RequestBody StartConversationRequest request) {
    return withCurrentTenant(
        tenantId -> {
          Conversation conversation =
              conversationService.startConversation(
                  tenantId, request.participantId(), request.channel());
          var location = URI.create("/api/v1/conversations/" + conversation.getId());
          return ResponseEntity.created(location).body(ConversationResponse.from(conversation));
        });
  }

  @GetMapping
  @Operation(summary = "List conversations for current tenant")
  public ResponseEntity<List<ConversationResponse>> list() {
    return withCurrentTenant(
        tenantId ->
            ResponseEntity.ok(
                conversationService.findByTenantId(tenantId).stream()
                    .map(ConversationResponse::from)
                    .toList()));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get conversation by ID")
  public ResponseEntity<ConversationResponse> get(@PathVariable UUID id) {
    Conversation conversation = conversationService.findById(id);
    return ResponseEntity.ok(ConversationResponse.from(conversation));
  }

  @PostMapping("/{id}/close")
  @Operation(summary = "Close a conversation")
  public ResponseEntity<ConversationResponse> close(@PathVariable UUID id) {
    Conversation conversation = conversationService.closeConversation(id);
    return ResponseEntity.ok(ConversationResponse.from(conversation));
  }

  @GetMapping("/{id}/events")
  @Operation(summary = "Get conversation event history")
  public ResponseEntity<List<EventResponse>> getHistory(@PathVariable UUID id) {
    return ResponseEntity.ok(
        conversationService.getHistory(id).stream().map(EventResponse::from).toList());
  }

  @PostMapping("/{id}/actions")
  @Operation(summary = "Propose an action in a conversation")
  public ResponseEntity<PendingActionResponse> proposeAction(
      @PathVariable UUID id, @RequestBody ProposeActionRequest request) {
    PendingAction action =
        conversationService.proposeAction(
            id, request.actionType(), request.details(), request.expiresAt());
    return ResponseEntity.ok(PendingActionResponse.from(action));
  }

  @PostMapping("/actions/{id}/confirm")
  @Operation(summary = "Confirm a pending action")
  public ResponseEntity<PendingActionResponse> confirmAction(@PathVariable UUID id) {
    PendingAction action = conversationService.confirmAction(id);
    return ResponseEntity.ok(PendingActionResponse.from(action));
  }

  @PostMapping("/actions/{id}/reject")
  @Operation(summary = "Reject a pending action")
  public ResponseEntity<PendingActionResponse> rejectAction(@PathVariable UUID id) {
    PendingAction action = conversationService.rejectAction(id);
    return ResponseEntity.ok(PendingActionResponse.from(action));
  }

  // --- DTOs ---

  public record ConversationResponse(
      UUID id,
      UUID tenantId,
      UUID participantId,
      String channel,
      String status,
      Instant startedAt,
      Instant createdAt) {
    public static ConversationResponse from(Conversation c) {
      return new ConversationResponse(
          c.getId(),
          c.getTenantId(),
          c.getParticipantId(),
          c.getChannel().name(),
          c.getStatus().name(),
          c.getStartedAt(),
          c.getCreatedAt());
    }
  }

  public record EventResponse(
      UUID id,
      UUID conversationId,
      Integer sequenceNumber,
      String eventType,
      String payload,
      Instant occurredAt) {
    public static EventResponse from(ConversationEvent e) {
      return new EventResponse(
          e.getId(),
          e.getConversationId(),
          e.getSequenceNumber(),
          e.getEventType(),
          e.getPayload(),
          e.getOccurredAt());
    }
  }

  public record PendingActionResponse(
      UUID id,
      UUID conversationId,
      String actionType,
      String status,
      String details,
      Instant expiresAt,
      Instant createdAt) {
    public static PendingActionResponse from(PendingAction a) {
      return new PendingActionResponse(
          a.getId(),
          a.getConversationId(),
          a.getActionType().name(),
          a.getStatus().name(),
          a.getDetails(),
          a.getExpiresAt(),
          a.getCreatedAtOverride());
    }
  }

  public record StartConversationRequest(UUID participantId, ChannelType channel) {}

  public record ProposeActionRequest(ActionType actionType, String details, Instant expiresAt) {}
}
