package com.emme.assistant.adapter.out.persistence.adapter;

import com.emme.assistant.adapter.out.persistence.entity.ConversationEventEntity;
import com.emme.assistant.adapter.out.persistence.mapper.ConversationEventPersistenceMapper;
import com.emme.assistant.adapter.out.persistence.repository.SpringDataConversationEventRepository;
import com.emme.assistant.application.port.out.ConversationEventRepository;
import com.emme.assistant.domain.model.ConversationEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ConversationEventPersistenceAdapter implements ConversationEventRepository {
  private final SpringDataConversationEventRepository repository;
  private final ConversationEventPersistenceMapper mapper;

  public ConversationEventPersistenceAdapter(
      SpringDataConversationEventRepository repository, ConversationEventPersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Optional<ConversationEvent> findLatestByConversationId(UUID conversationId) {
    return repository
        .findTopByConversationIdOrderBySequenceNumberDesc(conversationId)
        .map(mapper::toDomain);
  }

  @Override
  public List<ConversationEvent> findByConversationId(UUID conversationId) {
    return repository.findByConversationIdOrderBySequenceNumberAsc(conversationId).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public ConversationEvent save(ConversationEvent event) {
    ConversationEventEntity saved = repository.save(mapper.toEntity(event));
    return mapper.toDomain(saved);
  }
}
