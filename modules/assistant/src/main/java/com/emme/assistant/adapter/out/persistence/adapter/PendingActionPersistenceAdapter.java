package com.emme.assistant.adapter.out.persistence.adapter;

import com.emme.assistant.adapter.out.persistence.entity.PendingActionEntity;
import com.emme.assistant.adapter.out.persistence.mapper.PendingActionPersistenceMapper;
import com.emme.assistant.adapter.out.persistence.repository.SpringDataPendingActionRepository;
import com.emme.assistant.application.port.out.PendingActionRepository;
import com.emme.assistant.domain.model.ActionStatus;
import com.emme.assistant.domain.model.PendingAction;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PendingActionPersistenceAdapter implements PendingActionRepository {
  private final SpringDataPendingActionRepository repository;
  private final PendingActionPersistenceMapper mapper;

  public PendingActionPersistenceAdapter(
      SpringDataPendingActionRepository repository, PendingActionPersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Optional<PendingAction> findById(UUID actionId) {
    return repository.findById(actionId).map(mapper::toDomain);
  }

  @Override
  public List<PendingAction> findByTenantIdAndConversationIdAndStatus(
      UUID tenantId, UUID conversationId, ActionStatus status) {
    return repository
        .findByTenantIdAndConversationIdAndStatus(tenantId, conversationId, status)
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<PendingAction> findExpired(Instant now, ActionStatus status) {
    return repository.findByExpiresAtBeforeAndStatus(now, status).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public PendingAction save(PendingAction action) {
    PendingActionEntity saved = repository.save(mapper.toEntity(action));
    return mapper.toDomain(saved);
  }
}
