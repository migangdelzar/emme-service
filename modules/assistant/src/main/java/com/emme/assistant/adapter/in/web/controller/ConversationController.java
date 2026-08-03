package com.emme.assistant.adapter.in.web.controller;

import static com.emme.kernel.context.TenantContextHolder.withCurrentTenant;

import com.emme.assistant.adapter.in.web.mapper.AssistantWebMapper;
import com.emme.assistant.adapter.in.web.request.ProposeActionRequest;
import com.emme.assistant.adapter.in.web.request.StartConversationRequest;
import com.emme.assistant.adapter.in.web.response.ConversationResponse;
import com.emme.assistant.adapter.in.web.response.EventResponse;
import com.emme.assistant.adapter.in.web.response.PendingActionResponse;
import com.emme.assistant.api.command.CloseConversationCommand;
import com.emme.assistant.api.command.ConfirmPendingActionCommand;
import com.emme.assistant.api.command.RejectPendingActionCommand;
import com.emme.assistant.api.query.GetConversationHistoryQuery;
import com.emme.assistant.api.query.GetConversationQuery;
import com.emme.assistant.api.query.ListConversationsQuery;
import com.emme.assistant.api.usecase.CloseConversationUseCase;
import com.emme.assistant.api.usecase.ConfirmPendingActionUseCase;
import com.emme.assistant.api.usecase.GetConversationHistoryUseCase;
import com.emme.assistant.api.usecase.GetConversationUseCase;
import com.emme.assistant.api.usecase.ListConversationsUseCase;
import com.emme.assistant.api.usecase.ProposePendingActionUseCase;
import com.emme.assistant.api.usecase.RejectPendingActionUseCase;
import com.emme.assistant.api.usecase.StartConversationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
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
@RequestMapping(path = "/api/conversations", version = "1.0")
@Tag(name = "Conversations")
public class ConversationController {
  private final StartConversationUseCase start;
  private final ListConversationsUseCase list;
  private final GetConversationUseCase get;
  private final CloseConversationUseCase close;
  private final GetConversationHistoryUseCase history;
  private final ProposePendingActionUseCase propose;
  private final ConfirmPendingActionUseCase confirm;
  private final RejectPendingActionUseCase reject;

  public ConversationController(
      StartConversationUseCase start,
      ListConversationsUseCase list,
      GetConversationUseCase get,
      CloseConversationUseCase close,
      GetConversationHistoryUseCase history,
      ProposePendingActionUseCase propose,
      ConfirmPendingActionUseCase confirm,
      RejectPendingActionUseCase reject) {
    this.start = start;
    this.list = list;
    this.get = get;
    this.close = close;
    this.history = history;
    this.propose = propose;
    this.confirm = confirm;
    this.reject = reject;
  }

  @PostMapping
  @Operation(summary = "Start a new conversation")
  public ResponseEntity<ConversationResponse> start(
      @Valid @RequestBody StartConversationRequest request) {
    return withCurrentTenant(
        tenantId -> {
          var conversation = start.start(AssistantWebMapper.toCommand(tenantId, request));
          return ResponseEntity.created(URI.create("/api/conversations/" + conversation.id()))
              .body(ConversationResponse.from(conversation));
        });
  }

  @GetMapping
  public ResponseEntity<List<ConversationResponse>> list() {
    return withCurrentTenant(
        tenantId ->
            ResponseEntity.ok(
                list.list(new ListConversationsQuery(tenantId)).stream()
                    .map(ConversationResponse::from)
                    .toList()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ConversationResponse> get(@PathVariable UUID id) {
    return withCurrentTenant(
        tenantId ->
            get.get(new GetConversationQuery(tenantId, id))
                .map(ConversationResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build()));
  }

  @PostMapping("/{id}/close")
  public ResponseEntity<ConversationResponse> close(@PathVariable UUID id) {
    return withCurrentTenant(
        tenantId ->
            ResponseEntity.ok(
                ConversationResponse.from(
                    close.close(new CloseConversationCommand(tenantId, id)))));
  }

  @GetMapping("/{id}/events")
  public ResponseEntity<List<EventResponse>> getHistory(@PathVariable UUID id) {
    return withCurrentTenant(
        tenantId ->
            ResponseEntity.ok(
                history.get(new GetConversationHistoryQuery(tenantId, id)).stream()
                    .map(EventResponse::from)
                    .toList()));
  }

  @PostMapping("/{id}/actions")
  public ResponseEntity<PendingActionResponse> proposeAction(
      @PathVariable UUID id, @Valid @RequestBody ProposeActionRequest request) {
    return withCurrentTenant(
        tenantId ->
            ResponseEntity.ok(
                PendingActionResponse.from(
                    propose.propose(AssistantWebMapper.toCommand(tenantId, id, request)))));
  }

  @PostMapping("/actions/{id}/confirm")
  public ResponseEntity<PendingActionResponse> confirmAction(@PathVariable UUID id) {
    return withCurrentTenant(
        tenantId ->
            ResponseEntity.ok(
                PendingActionResponse.from(
                    confirm.confirm(new ConfirmPendingActionCommand(tenantId, id)))));
  }

  @PostMapping("/actions/{id}/reject")
  public ResponseEntity<PendingActionResponse> rejectAction(@PathVariable UUID id) {
    return withCurrentTenant(
        tenantId ->
            ResponseEntity.ok(
                PendingActionResponse.from(
                    reject.reject(new RejectPendingActionCommand(tenantId, id)))));
  }
}
