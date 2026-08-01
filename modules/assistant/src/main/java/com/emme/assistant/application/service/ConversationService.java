package com.emme.assistant.application.service;

import com.emme.assistant.adapter.out.persistence.entity.ConversationEntity;
import com.emme.assistant.adapter.out.persistence.entity.ConversationEventEntity;
import com.emme.assistant.adapter.out.persistence.entity.PendingActionEntity;
import com.emme.assistant.adapter.out.persistence.repository.SpringDataConversationEventRepository;
import com.emme.assistant.adapter.out.persistence.repository.SpringDataConversationRepository;
import com.emme.assistant.adapter.out.persistence.repository.SpringDataPendingActionRepository;
import com.emme.assistant.domain.model.ActionStatus;
import com.emme.assistant.domain.model.ActionType;
import com.emme.assistant.domain.model.ConversationStatus;
import com.emme.kernel.type.ChannelType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ConversationService {

  private final SpringDataConversationRepository conversationRepository;
  private final SpringDataConversationEventRepository eventRepository;
  private final SpringDataPendingActionRepository actionRepository;

  public ConversationService(
      SpringDataConversationRepository conversationRepository,
      SpringDataConversationEventRepository eventRepository,
      SpringDataPendingActionRepository actionRepository) {
    this.conversationRepository = conversationRepository;
    this.eventRepository = eventRepository;
    this.actionRepository = actionRepository;
  }

  public ConversationEntity startConversation(
      UUID tenantId, UUID participantId, ChannelType channel) {
    ConversationEntity conversation = new ConversationEntity(tenantId, participantId, channel);
    return conversationRepository.save(conversation);
  }

  public ConversationEntity closeConversation(UUID conversationId) {
    ConversationEntity conversation =
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

  public ConversationEventEntity addEvent(UUID conversationId, String eventType, String payload) {
    ConversationEntity conversation =
        conversationRepository
            .findById(conversationId)
            .orElseThrow(
                () -> new IllegalArgumentException("Conversation not found: " + conversationId));

    int nextSeq =
        eventRepository
            .findTopByConversationIdOrderBySequenceNumberDesc(conversationId)
            .map(e -> e.getSequenceNumber() + 1)
            .orElse(1);

    ConversationEventEntity event =
        new ConversationEventEntity(
            conversation.getTenantId(), conversationId, nextSeq, eventType, payload);
    return eventRepository.save(event);
  }

  @Transactional(readOnly = true)
  public List<ConversationEventEntity> getHistory(UUID conversationId) {
    return eventRepository.findByConversationIdOrderBySequenceNumberAsc(conversationId);
  }

  public PendingActionEntity proposeAction(
      UUID conversationId, ActionType type, String details, Instant expiresAt) {
    ConversationEntity conversation =
        conversationRepository
            .findById(conversationId)
            .orElseThrow(
                () -> new IllegalArgumentException("Conversation not found: " + conversationId));

    PendingActionEntity action =
        new PendingActionEntity(
            conversation.getTenantId(), conversationId, type, details, expiresAt);
    return actionRepository.save(action);
  }

  public PendingActionEntity confirmAction(UUID actionId) {
    PendingActionEntity action =
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

  public PendingActionEntity rejectAction(UUID actionId) {
    PendingActionEntity action =
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
    List<PendingActionEntity> stale =
        actionRepository.findByExpiresAtBeforeAndStatus(now, ActionStatus.PENDING);
    for (PendingActionEntity a : stale) {
      a.setStatus(ActionStatus.EXPIRED);
      actionRepository.save(a);
    }
    return stale.size();
  }

  @Transactional(readOnly = true)
  public List<PendingActionEntity> getActiveActions(UUID conversationId) {
    return actionRepository.findByConversationIdAndStatus(conversationId, ActionStatus.PENDING);
  }

  @Transactional(readOnly = true)
  public List<ConversationEntity> findByTenantId(UUID tenantId) {
    return conversationRepository.findByTenantId(tenantId);
  }

  @Transactional(readOnly = true)
  public ConversationEntity findById(UUID id) {
    return conversationRepository
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + id));
  }
}
