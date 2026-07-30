package com.emme.assistant.application;

import com.emme.assistant.entity.ActionStatus;
import com.emme.assistant.entity.ActionType;
import com.emme.assistant.entity.Conversation;
import com.emme.assistant.entity.ConversationEvent;
import com.emme.assistant.entity.ConversationEventRepository;
import com.emme.assistant.entity.ConversationRepository;
import com.emme.assistant.entity.ConversationStatus;
import com.emme.assistant.entity.PendingAction;
import com.emme.assistant.entity.PendingActionRepository;
import com.emme.kernel.type.ChannelType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ConversationService {

  private final ConversationRepository conversationRepository;
  private final ConversationEventRepository eventRepository;
  private final PendingActionRepository actionRepository;

  public ConversationService(
      ConversationRepository conversationRepository,
      ConversationEventRepository eventRepository,
      PendingActionRepository actionRepository) {
    this.conversationRepository = conversationRepository;
    this.eventRepository = eventRepository;
    this.actionRepository = actionRepository;
  }

  public Conversation startConversation(UUID tenantId, UUID participantId, ChannelType channel) {
    Conversation conversation = new Conversation(tenantId, participantId, channel);
    return conversationRepository.save(conversation);
  }

  public Conversation closeConversation(UUID conversationId) {
    Conversation conversation =
        conversationRepository
            .findById(conversationId)
            .orElseThrow(
                () -> new IllegalArgumentException("Conversation not found: " + conversationId));
    if (conversation.getStatus() != ConversationStatus.ACTIVE) {
      throw new IllegalStateException("Conversation is not active: " + conversationId);
    }
    conversation.setStatus(ConversationStatus.CLOSED);
    return conversationRepository.save(conversation);
  }

  public ConversationEvent addEvent(UUID conversationId, String eventType, String payload) {
    Conversation conversation =
        conversationRepository
            .findById(conversationId)
            .orElseThrow(
                () -> new IllegalArgumentException("Conversation not found: " + conversationId));

    int nextSeq =
        eventRepository
            .findTopByConversationIdOrderBySequenceNumberDesc(conversationId)
            .map(e -> e.getSequenceNumber() + 1)
            .orElse(1);

    ConversationEvent event =
        new ConversationEvent(
            conversation.getTenantId(), conversationId, nextSeq, eventType, payload);
    return eventRepository.save(event);
  }

  @Transactional(readOnly = true)
  public List<ConversationEvent> getHistory(UUID conversationId) {
    return eventRepository.findByConversationIdOrderBySequenceNumberAsc(conversationId);
  }

  public PendingAction proposeAction(
      UUID conversationId, ActionType type, String details, Instant expiresAt) {
    Conversation conversation =
        conversationRepository
            .findById(conversationId)
            .orElseThrow(
                () -> new IllegalArgumentException("Conversation not found: " + conversationId));

    PendingAction action =
        new PendingAction(conversation.getTenantId(), conversationId, type, details, expiresAt);
    return actionRepository.save(action);
  }

  public PendingAction confirmAction(UUID actionId) {
    PendingAction action =
        actionRepository
            .findById(actionId)
            .orElseThrow(
                () -> new IllegalArgumentException("PendingAction not found: " + actionId));
    if (action.getStatus() != ActionStatus.PENDING) {
      throw new IllegalStateException("Action not in PENDING state: " + actionId);
    }
    action.setStatus(ActionStatus.CONFIRMED);
    action.setStatus(ActionStatus.EXECUTED);
    return actionRepository.save(action);
  }

  public PendingAction rejectAction(UUID actionId) {
    PendingAction action =
        actionRepository
            .findById(actionId)
            .orElseThrow(
                () -> new IllegalArgumentException("PendingAction not found: " + actionId));
    if (action.getStatus() != ActionStatus.PENDING) {
      throw new IllegalStateException("Action not in PENDING state: " + actionId);
    }
    action.setStatus(ActionStatus.REJECTED);
    return actionRepository.save(action);
  }

  public int expireStaleActions() {
    Instant now = Instant.now();
    List<PendingAction> stale =
        actionRepository.findByExpiresAtBeforeAndStatus(now, ActionStatus.PENDING);
    for (PendingAction a : stale) {
      a.setStatus(ActionStatus.EXPIRED);
      actionRepository.save(a);
    }
    return stale.size();
  }

  @Transactional(readOnly = true)
  public List<PendingAction> getActiveActions(UUID conversationId) {
    return actionRepository.findByConversationIdAndStatus(conversationId, ActionStatus.PENDING);
  }

  @Transactional(readOnly = true)
  public List<Conversation> findByTenantId(UUID tenantId) {
    return conversationRepository.findByTenantId(tenantId);
  }

  @Transactional(readOnly = true)
  public Conversation findById(UUID id) {
    return conversationRepository
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + id));
  }
}
